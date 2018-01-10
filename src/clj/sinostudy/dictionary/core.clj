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

(defn cl-def?
  "Determine if a dictionary definition is actually a list of classifiers."
  [definition]
  (str/starts-with? definition "CL:"))

(defn cl-def-entry?
  "Determine if the entry's :definition contains classifiers."
  [entry]
  (some cl-def? (:definition entry)))

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
  "Merge entry (e.g. name entry) into matching existing entries in dict.
  This both merges the definition into other entries and removes the old entry.
  In case there is only one entry, nothing happens.
  Returns dict with the entry merged in."
  [key-type dict entry]
  (let [key     (get entry key-type)
        entries (get dict key)]
    (if (= 1 (count entries))
      dict
      (loop [matches* (filter (partial matches-entry entry) entries)
             entries* entries]
        (let [match (first matches*)]
          (cond
            (nil? match) (assoc dict key entries*)
            (= match entry) (recur (rest matches*) (disj entries* match))
            :else (let [match-def (:definition match)
                        entry-def (:definition entry)
                        new-def   (set/union match-def entry-def)
                        new-entry (assoc match :definition new-def)]
                    (recur (rest matches*) (-> entries*
                                               (disj match)
                                               (conj new-entry))))))))))

(defn merge-entries
  "Merge definitions of entries into matching entries in dict.
  Returns the modified dict."
  [key-type dict entries]
  (loop [dict*    dict
         entries* entries]
    (if-let [entry (first entries*)]
      (recur (merge-entry key-type dict* entry) (rest entries*))
      dict*)))

;; TODO: remove "(!old) variant of X" if the referenced char is identical
;;       (note: trad and simp may differ!)
;; TODO: merge entries that only differ in definitions, trad and simp may differ
;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable
;; TODO: make the pattern "classifier for X" prominent (CL)
;; TODO: make the pattern "to X" prominent (V)
;; TODO: do something about weird simplifications like 制 (dual entries in simp)
;;       perhaps simply merge while constructing the list and remove "variant
;;       of X" from definitions?
;; TODO: merge duoyinci, perhaps just conj Pinyin and defs
;;       e.g. {... :pinyin [[...] [...]], :definition [[...] [...]]}


(defn load-entries
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter entry?)
         (map extract-entry)
         (vec))))

(defn compile-dict
  "Create a dictionary map from the entries with keys determined by key-type,
  this being the field in the entry that must serve as key (e.g. :traditional)."
  [key-type entries]
  (reduce (partial add-entry key-type) {} entries))

(defn compile-dicts
  "Create a map of dictionary maps with different look-up key-types."
  [key-types entries]
  (println "compiling" (count key-types) "dicts from" (count entries) "entries")
  (let [make-dict (fn [key-type] [key-type (compile-dict key-type entries)])]
    (into {} (map make-dict key-types))))

(defn mod-dicts
  "Merge the entries of merges into each dictionary map."
  [merges dicts]
  (println "merging" (count merges) "entries into" (count dicts) "dicts")
  (loop [keys (keys dicts)
         dicts* dicts]
    (if-let [key (first keys)]
      (recur
        (rest keys)
        (assoc dicts* key (merge-entries key (get dicts* key) merges)))
      dicts*)))

(defn isolate-classifiers
  "Moves the classifiers of an entry from :definition to :classifiers."
  [entry]
  (if (cl-def-entry? entry)
    (let [defs        (:definition entry)
          cl-defs     (filter cl-def? defs)
          new-defs    (set/difference defs cl-defs)
          classifiers (set (flatten (map dict/get-refs cl-defs)))]
      (if classifiers
        (-> entry
            (assoc :definition new-defs)
            (assoc :classifiers classifiers))
        entry))
    entry))

(defn load-dicts
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [entries key-types]
  (let [name-entries (filter name-entry? entries)]
    (->> entries
         (map isolate-classifiers)
         (compile-dicts key-types)
         (mod-dicts name-entries))))

(defn look-up
  "Look up the specified word in each dictionary map and merge the results."
  [word dicts]
  (let [check-dict (fn [n] (get (nth (vals dicts) n) word))]
    (->> (map check-dict (range (count dicts)))
         (filter (comp not nil?))
         (apply set/union))))
