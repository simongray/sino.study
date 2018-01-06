(ns sinostudy.dictionary.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [sinostudy.dictionary.common :as dict]
            [sinostudy.pinyin.eval :as pe]))

(defn entry?
  "Determine if a line is a dictionary entry."
  [line]
  (not (str/starts-with? line "#")))

(defn split-def
  "Split the CC-CEDICT definition string into separate, unique parts."
  [definition]
  (set (str/split definition #"/")))

(defn u:->umlaut
  "Replace the CC-CEDICT substitute u: with the proper Pinyin ü."
  [pinyin]
  (str/replace pinyin "u:" "ü"))

(defn join-abbr
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

(defn pinyin-seq
  "Transform the CC-CEDICT Pinyin string into a seq of diacritised syllables."
  [pinyin]
  (map neutral-as-0 (str/split pinyin #" ")))

;; TODO: use this during comparisons and for storing pinyin-key
;; relatively common punctuation that should be ignored during comparisons
(def punctuation
  #{"·" ","})

;; CC-CEDICT mistakes and oddities that are ignored during import
(def cc-cedict-oddities
  #{"xx5" "xx" "ging1"})

(defn preprocess
  "Apply preprocessing functions to a CC-CEDICT Pinyin string."
  [s]
  (if (contains? cc-cedict-oddities s)
    []
    (-> s
        u:->umlaut
        join-abbr
        pinyin-seq
        vec)))

(defn extract-entry
  "Extract the constituents of a matching dictionary entry according to regex:
  TRADITIONAL SIMPLIFIED [PINYIN] /DEFINITION/"
  [line]
  (let [pattern #"^([^ ]+) ([^ ]+) \[([^]]+)\] /(.+)/"
        [_ trad simp pinyin definition :as entry] (re-matches pattern line)]
    (when entry
      (let [pinyin-key        (dict/pinyin-key pinyin)
            pinyin+diacritics (preprocess pinyin)
            definitions       (split-def definition)]
        [trad simp pinyin-key pinyin+diacritics definitions]))))

;; Note: do not remove pinyin-key -- it is not unused, just indirectly used!
(defn add-entry
  "Add (or extend) an entry in the dictionary map; n marks the look-up key."
  [n dictionary [trad simp pinyin-key pinyin definition :as entry]]
  (let [key   (nth entry n)
        entry {:traditional trad
               :simplified  simp
               :pinyin      pinyin
               :definition  definition}]
    (if-let [entries (get dictionary key)]
      (assoc dictionary key (conj entries entry))
      (assoc dictionary key #{entry}))))

(def compile-dictionaries
  "Reduce into a vector of 3 dictionary maps with different look-up keys:
  traditional, simplified, and basic Pinyin."
  (let [add-trad   (partial add-entry 0)
        add-simp   (partial add-entry 1)
        add-pinyin (partial add-entry 2)]
    (juxt (partial reduce add-trad {})
          (partial reduce add-simp {})
          (partial reduce add-pinyin {}))))

;; TODO: remove entries w/ "surname X" def and add tag to referenced entry
;; TODO: merge entries where pinyin is capitalised (e.g. Ming2 with ming2)
;; TODO: remove "(!old) variant of X" if the referenced char is identical
;;       (note: trad and simp may differ!)
;; TODO: merge entries that only differ in definitions, trad and simp may differ
;; TODO: autolinking of common patterns Trad|Simp[Pin] and Char[Pin]
;; TODO: remove CL pattern defs and add classifier list to entry instead
;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable

(defn load-dictionaries
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter entry?)
         (map extract-entry)
         compile-dictionaries)))

(defn look-up
  "Look up the specified word in each dictionary map and merge the results."
  [word dictionaries]
  (let [check-dict (fn [n] (get (nth dictionaries n) word))]
    (->> (map check-dict (range (count dictionaries)))
         (filter (comp not nil?))
         (apply set/union))))
