(ns sinostudy.dictionary.core
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [sinostudy.dictionary.defaults :as dd]
            [sinostudy.rim.core :as rim]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]))

;; TODO: merge entries that only differ in definitions, trad and simp may differ
;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable
;; TODO: make the pattern "classifier for X" prominent (CL)
;; TODO: make the pattern "to X" prominent (V)
;; TODO: do something about weird simplifications like 制 (dual entries in simp)
;;       perhaps simply merge while constructing the list and remove "variant
;;       of X" from definitions?
;; TODO: merge duoyinci, perhaps just conj Pinyin and defs
;;       e.g. {... :pinyin [[...] [...]], :definition [[...] [...]]}
;; TODO: tag radicals, e.g. def = "Kangxi radical 206" or just from a list

(def hanzi-ref-pattern
  "A pattern used in CC-CEDICT to embed a hanzi reference, e.g. 樁|桩[zhuang1]."
  #"[^ ,:\[a-zA-Z0-9]+\[[^\]]+\]+")

(defn hanzi-ref->map
  "Transform a hanzi-ref-str into a Clojure map."
  [hanzi-ref-str]
  (let [[hanzi-str pinyin-str] (str/split hanzi-ref-str #"\[|\]")
        hanzi       (str/split hanzi-str #"\|")
        pinyin      (str/split pinyin-str #" ")
        traditional (first hanzi)
        simplified  (if (second hanzi) (second hanzi) traditional)]
    {dd/trad   traditional
     dd/simp   simplified
     dd/pinyin pinyin}))

(defn hanzi-refs
  "Get all of the hanzi reference in s as Clojure maps."
  [s]
  (map hanzi-ref->map (re-seq hanzi-ref-pattern s)))

(defn handle-hanzi-refs
  "Apply function f to all hanzi refs in definition."
  [definition f]
  (let [hanzi-refs (hanzi-refs definition)]
    (if (empty? hanzi-refs)
      definition
      (interleave (str/split definition hanzi-ref-pattern)
                  (map f hanzi-refs)))))

;; TODO: 唎 in traditional special case
(defn variant-def?
  "Is this definition a reference to a common char/word it is a variant of?"
  [definition]
  (not (nil? (re-find #"variant of" definition))))

(defn variant-entry?
  "Is this entry a variant of a more common char/word?"
  [entry]
  (let [definitions (dd/defs entry)]
    (some variant-def? definitions)))

(defn contains-defs?
  "Does entry contain the definitions of variant-entry?"
  [variant-entry entry]
  (let [rest-defs (partial filter (complement variant-def?))
        e1-defs   (rest-defs (dd/defs variant-entry))
        e2-defs   (set (dd/defs entry))]
    (every? (partial contains? e2-defs) e1-defs)))

(defn same-hanzi?
  "Does the script (:simplified or :traditional) of entry match variant-entry?"
  [script variant-entry entry]
  (= (get variant-entry script) (get entry script)))

(defn false-variants
  "Find false variants (usually an artifact of using Simplified Chinese)."
  [script entries]
  (let [variants     (filter variant-entry? entries)
        others       (filter (complement variant-entry?) entries)
        same-hanzi?* (partial same-hanzi? script)
        get-matches  #(rim/all-matches % others same-hanzi?* contains-defs?)]
    (filter (comp not empty? get-matches) variants)))

;; TODO: figure out a better solution for variants and false variants
(defn tag-false-variants
  "Tag false variants in a list of entries when comparing the current script.
  This is most likely to happen when the script is :simplified."
  [script entries]
  (let [false-variants (set (false-variants script entries))
        tag-variant    (fn [entry]
                         (if (contains? false-variants entry)
                           (assoc entry :false-variant #{script})
                           entry))]
    (map tag-variant entries)))

;; using CC-CEDICT Pinyin directly for dictionary look-ups is too strict
(defn pinyin-key
  "Converts CC-CEDICT Pinyin string into a plain form for use as a map key.
  Spaces and tone digits are removed entirely and the text is made lowercase."
  [s]
  (-> s
      (str/replace " " "")
      (str/replace "·" "")                                  ; middle dot
      (str/replace "," "")
      (str/replace #"\d" "")
      str/lower-case))

(def pinyin>case>character
  "Order first by Pinyin, then by case, then by character."
  (juxt #(str/lower-case (str/join (dd/pinyin %))) dd/pinyin dd/trad))

(defn sort-defs
  "Sort the definitions of a dictionary entry."
  [entry]
  (let [sorted-defs (sort (dd/defs entry))]
    (assoc entry dd/defs sorted-defs)))

(defn digits->diacritics*
  "Convert (only) Pinyin+digit syllables to diacritics."
  [s]
  (if (pe/pinyin-block+digits? s)
    (p/digits->diacritics s)
    s))

(defn add-diacritics
  "Add diacritics to the Pinyin of an entry."
  [entry]
  (let [pinyin+diacritics (map digits->diacritics* (dd/pinyin entry))]
    (assoc entry dd/pinyin pinyin+diacritics)))

(defn prepare-entries
  "Sort a list of dictionary entries, including entry definitions.
  Then convert the tone digits to diacritics."
  [entries]
  (->> entries
       (map sort-defs)
       (sort-by pinyin>case>character)
       (map add-diacritics)
       (vec)))                                              ; allow fetching by index

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
      (let [pinyin-key        (pinyin-key pinyin)
            pinyin+diacritics (preprocess pinyin)
            definitions       (split-def definition)]
        {dd/trad       trad
         dd/simp       simp
         dd/pinyin     pinyin+diacritics
         dd/pinyin-key pinyin-key
         dd/defs       definitions}))))

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

(defn cl-entry?
  "Determine if the entry's :definitions contain classifiers."
  [entry]
  (some cl-def? (dd/defs entry)))

(defn name-entry?
  "Determine is a dictionary entry is a name entry."
  [entry]
  (when-let [first-letter (first (first (dd/pinyin entry)))]
    #?(:clj  (Character/isUpperCase ^char first-letter)
       :cljs (not= first-letter (.toLowerCase first-letter)))))

(defn matches-pinyin
  "True if the :pinyin of both entries is equal, disregarding case."
  [p1 p2]
  (let [lower-case= #(= (str/lower-case %1) (str/lower-case %2))]
    (every? true? (map lower-case= p1 p2))))

(defn matches-entry
  "True if everything matches except the definitions and the :pinyin case."
  [e1 e2]
  (and (= (dd/simp e1) (dd/simp e2))
       (= (dd/trad e1) (dd/trad e2))
       (matches-pinyin (dd/pinyin e1) (dd/pinyin e2))))

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
            :else (let [match-def (dd/defs match)
                        entry-def (dd/defs entry)
                        new-def   (set/union match-def entry-def)
                        new-entry (assoc match dd/defs new-def)]
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
  (loop [keys   (keys dicts)
         dicts* dicts]
    (if-let [key (first keys)]
      (recur
        (rest keys)
        (assoc dicts* key (merge-entries key (get dicts* key) merges)))
      dicts*)))

(defn detach-cls
  "Moves the classifiers of an entry from :definitions to :classifiers."
  [entry]
  (if (cl-entry? entry)
    (let [defs     (dd/defs entry)
          cl-defs  (filter cl-def? defs)
          new-defs (set/difference defs cl-defs)
          cls      (set (flatten (map hanzi-refs cl-defs)))]
      (if cls
        (-> entry
            (assoc dd/defs new-defs)
            (assoc dd/cls cls))
        entry))
    entry))

(defn load-dicts
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [entries key-types]
  (let [name-entries (filter name-entry? entries)]
    (->> entries
         (map detach-cls)
         (compile-dicts key-types)
         (mod-dicts name-entries))))

(defn look-up
  "Look up the specified word in each dictionary map and merge the results."
  [word dicts]
  (let [check-dict (fn [n] (get (nth (vals dicts) n) word))]
    (->> (map check-dict (range (count dicts)))
         (filter (comp not nil?))
         (apply set/union))))
