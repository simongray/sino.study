(ns sinostudy.dictionary.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [sinostudy.dictionary.common :as dict]
            [sinostudy.pinyin.eval :as pe]))

(def look-up-keys
  [:traditional :simplified :pinyin-key])

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
  "Extract the constituents of a matching CC-CEDICT dictionary entry.
  Returns a [pinyin-key {entry}] vector (the pinyin-key is used for look-ups)."
  [line]
  (let [pattern #"^([^ ]+) ([^ ]+) \[([^]]+)\] /(.+)/"
        [_ trad simp pinyin definition :as entry] (re-matches pattern line)]
    (when entry
      (let [pinyin-key        (dict/pinyin-key pinyin)
            pinyin+diacritics (preprocess pinyin)
            definitions       (split-def definition)]
        {:traditional trad
         :simplified  simp
         :pinyin      pinyin+diacritics
         :pinyin-key  pinyin-key
         :definition  definitions}))))

;; Note: do not remove pinyin-key -- it is not unused, just indirectly used!
(defn add-entry
  "Add (or extend) an entry in the dictionary map; key marks the look-up key.
  Pinyin is a special case, as the look-up key is further processed."
  [key-type m entry]
  (let [key (get entry key-type)]
    (if-let [entries (get m key)]
      (assoc m key (conj entries entry))
      (assoc m key #{entry}))))

(defn compile-dictionary
  "Create a dictionary map from the entries with keys determined by key-type,
  this being the field in the entry that must serve as key (e.g. :traditional)."
  [entries key-type]
  (reduce (partial add-entry key-type) {} entries))

(defn compile-dictionaries
  "Create a map of dictionary maps with different look-up key-types."
  [entries key-types]
  (let [make-dict #(vector % (compile-dictionary entries %))]
    (into {} (map make-dict key-types))))

(defn name-entry?
  "Determine is a dictionary entry is a name entry."
  [entry]
  (when-let [first-letter (first (first (:pinyin entry)))]
    (Character/isUpperCase ^char first-letter)))

(defn matches-pinyin
  "True if the :pinyin of both entries is equal, disregarding case."
  [p1 p2]
  (let [lower-case= #(= (str/lower-case %1) (str/lower-case %2))]
    (every? true? (map lower-case= p1 p2))))

(defn matches-entry
  "True if everything matches except the definitions and the :pinyin case."
  [e1 e2]
  (and (= (:simplified e1) (:simplified e2))
       (= (:traditional e1) (:traditional e2))
       (matches-pinyin (:pinyin e1) (:pinyin e2))))

(defn merge-entry
  "Merges an entry (e.g. a name entry) into matching existing entries."
  [dictionary key-type entry]
  (let [key     (get entry key-type)
        entries (get dictionary key)
        matches (filter (partial matches-entry entry) entries)]
    (loop [matches* matches
           entries* entries]
      (if-let [match (first matches*)]
        (let [new-def (set/union (:definition match) (:definition entry))
              new-entry (assoc match :definition new-def)]
          (recur (rest matches*) (-> entries* (disj match) (conj new-entry))))
        (assoc dictionary key entries*)))))

;; TODO: remove entries w/ "surname X" def and add tag to referenced entry
;; TODO: merge entries where pinyin is capitalised (e.g. Ming2 with ming2)
;; TODO: remove "(!old) variant of X" if the referenced char is identical
;;       (note: trad and simp may differ!)
;; TODO: merge entries that only differ in definitions, trad and simp may differ
;; TODO: autolinking of common patterns Trad|Simp[Pin] and Char[Pin]
;; TODO: remove CL pattern defs and add classifier list to entry instead
;;       CL:條|条[tiao2],股[gu3],根[gen1]
;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable
;; TODO: make the pattern "classifier for X" prominent (CL)
;; TODO: make the pattern "to X" prominent (V)

(defn load-entries
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter entry?)
         (map extract-entry)
         (vec))))

(defn load-dictionaries
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [entries key-types]
  (let [name-entries  (filter name-entry? entries)
        other-entries (filter (complement name-entry?) entries)]
    (compile-dictionaries other-entries key-types)))

(defn look-up
  "Look up the specified word in each dictionary map and merge the results."
  [word dictionaries]
  (let [check-dict (fn [n] (get (nth (vals dictionaries) n) word))]
    (->> (map check-dict (range (count dictionaries)))
         (filter (comp not nil?))
         (apply set/union))))
