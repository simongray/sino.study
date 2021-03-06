(ns sinostudy.dictionary.load
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [clj-json.core :as json]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]))

;;;; CC-CEDICT

(defn- u:->umlaut
  "Replace the CC-CEDICT substitute u: with the proper Pinyin ü."
  [pinyin]
  (str/replace pinyin "u:" "ü"))

(defn- join-abbr
  "Join the uppercase letters in a CC-CEDICT Pinyin string into blocks."
  [pinyin]
  (let [abbr-letters  #"([A-Z]( [A-Z])+)( |$)"
        remove-spaces #(str (str/replace (% 1) " " "") (% 3))]
    (str/replace pinyin abbr-letters remove-spaces)))

(defn neutral-as-0
  "Convert the neutral tone digits (represented as 5 in CC-CEDICT) to 0.
  This ensures that the Pinyin strings are alphabetically sortable."
  [s]
  (if (pe/pinyin-block+digits? s)
    (str/replace s "5" "0")
    s))

(defn split-defs
  "Split the CC-CEDICT definition string into separate, unique parts."
  [definition]
  (set (str/split definition #"/")))

(defn line->cedict-listing
  "Extract the constituents of a line in a CC-CEDICT dictionary file.
  Returns a map representation suitable for use as a dictionary entry."
  [line]
  (let [pattern #"^([^ ]+) ([^ ]+) \[([^]]+)\] /(.+)/"
        [_ trad simp pinyin defs :as entry] (re-matches pattern line)]
    (when entry
      (let [pinyin* (u:->umlaut (neutral-as-0 pinyin))]
        {:traditional           trad
         :simplified            simp
         :pinyin                (join-abbr pinyin*)
         :pinyin-key            (d/pinyin-key (str/replace pinyin* #"\d" ""))
         :pinyin+digits-key     (d/pinyin-key pinyin*)
         :pinyin+diacritics-key (d/pinyin-key (p/digits->diacritics pinyin*))
         :definitions           (split-defs defs)}))))

(defn load-cedict
  "Load the listings of a CC-CEDICT dictionary file into Clojure maps."
  [file]
  (with-open [reader (io/reader file)]
    (->> (doall (line-seq reader))
         (remove #(str/starts-with? % "#"))
         (map line->cedict-listing))))


;;;; WORD FREQUENCY

(defn line->freq-listing
  "Extract the constituents of a line in a CC-CEDICT dictionary file.
  Returns a map representation suitable for use as a dictionary entry."
  [line]
  (let [re #"^([^ ]+) ([^ ]+) ([^ ]+)"
        [_ _ freq word :as entry] (re-matches re line)]
    (when entry
      {:frequency (Double/parseDouble freq)
       :word      word})))

(defn normalise
  "Normalise the frequency of a freq-listing."
  [max-freq freq-listing]
  (assoc freq-listing :frequency (/ (:frequency freq-listing)
                                    max-freq)))

(defn load-freq-dict
  "Load the listings of 1 or more frequency files into a Clojure map."
  ([file]
   (with-open [reader (io/reader file)]
     (let [raw-listings (->> (doall (line-seq reader))
                             (filter #(re-find #"^\d+ " %))
                             (map line->freq-listing)
                             (filter (comp not nil?)))
           max-freq     (:frequency (first raw-listings))]
       (->> raw-listings
            (map (partial normalise max-freq))
            (reduce #(assoc %1 (:word %2) (:frequency %2)) {})))))
  ([file & files]
   (let [m  (load-freq-dict file)
         ms (map load-freq-dict files)]
     (reduce (partial merge-with #(/ (+ %1 %2) 2)) m ms))))


;;;; CHARACTER COMPOSITION, ETYMOLOGY, ETC.

(defn load-makemeahanzi
  "Load the listings of a makemeahanzi file into a Clojure map."
  [file]
  (with-open [reader (io/reader file)]
    (let [raw-listings (->> (doall (line-seq reader))
                            (map json/parse-string))]
      (reduce #(assoc %1 (get %2 "character") %2) {} raw-listings))))


;;;; EXAMPLE SENTENCES + THEIR RELATIONS AND METADATA
(defn load-sentences
  [sentences-file links-file]
  (with-open [sentences-reader (io/reader sentences-file)
              links-reader     (io/reader links-file)]
    (let [entries (->> (csv/read-csv sentences-reader :separator \tab :quote \^)
                       ;(take-nth 1000)                 ; TODO: remove
                       (map (partial take 3))
                       (filter (comp #{"eng" "cmn"} second))
                       (doall))

          ;; We only want to keep stuff around that is present in both eng/cmn.
          cmn-ids (->> entries
                       (filter (comp #{"cmn"} second))
                       (set))

          ;; It seems like the links in this dataset include both directions.
          links   (->> (csv/read-csv links-reader :separator \tab)
                       (filter (comp cmn-ids second))
                       (doall))]                            ;TODO: first, second?
      {:entries (count entries)
       :cmn-ids (count cmn-ids)
       :links   (count links)})))


(defn load-test
  []
  (load-sentences
    (str (System/getProperty "user.home") "/" "Code/sinostudy-data/"
         "tatoeba/sentences_detailed.csv")
    (str (System/getProperty "user.home") "/" "Code/sinostudy-data/"
         "tatoeba/links.csv")))

;;;; FULL DICTIONARY

(defn- in-home
  "Expands to the current user's home directory + s."
  [s]
  (str (System/getProperty "user.home") "/" s))

;; Note: dict compilation requires the sinostudy-data git repo to be located in:
;; ~/Code/sinostudy-data
(defn load-dict
  []
  (let [data         #(in-home (str "Code/sinostudy-data/" %))
        listings     (load-cedict
                       (data "cedict_ts.u8"))
        freq-dict    (load-freq-dict
                       (data "frequency/internet-zh.num.txt")
                       (data "frequency/giga-zh.num.txt"))
        makemeahanzi (load-makemeahanzi
                       (data "makemeahanzi/dictionary.txt"))]
    (d/create-dict listings freq-dict makemeahanzi)))
