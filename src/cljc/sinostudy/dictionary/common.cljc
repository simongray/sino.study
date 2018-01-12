(ns sinostudy.dictionary.common
  (:require [clojure.string :as str]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]))

(def hanzi-ref
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
    {:traditional traditional
     :simplified  simplified
     :pinyin      pinyin}))

(defn get-refs
  "Get all of the hanzi reference in s as Clojure maps."
  [s]
  (map hanzi-ref->map (re-seq hanzi-ref s)))

;; TODO: 唎 in traditional special case
(defn variant-def?
  "Is this definition a reference to a common char/word it is a variant of?"
  [definition]
  (not (nil? (re-find #"variant of" definition))))

(defn variant-entry?
  "Is this entry a variant of a more common char/word?"
  [entry]
  (let [definitions (:definition entry)]
    (some variant-def? definitions)))

(defn match?
  "Test every predicate function in preds on objects x and y.
  Returns true (= x and y match) if all function calls return true."
  [x y & preds]
  (every? (fn [pred] (pred x y)) preds))

(defn all-matches
  "Get all entries in xs that match x based on predicate functions in preds."
  [x xs & preds]
  (filter (fn [y] (apply match? x y preds)) xs))

(defn contains-defs?
  "Does entry contain the definitions of variant-entry?"
  [variant-entry entry]
  (let [rest-defs (partial filter (complement variant-def?))
        e1-defs   (rest-defs (:definition variant-entry))
        e2-defs   (set (:definition entry))]
    (every? (partial contains? e2-defs) e1-defs)))

(defn same-hanzi?
  "Does the script (:simplified or :traditional) of entry match variant-entry?"
  [script variant-entry entry]
  (= (get variant-entry script) (get entry script)))

(defn false-variants
  "Find false variants (usually an artifact of using Simplified Chinese)."
  [script entries]
  (let [variants         (filter variant-entry? entries)
        others           (filter (complement variant-entry?) entries)
        same-hanzi?*     (partial same-hanzi? script)
        matching-entries #(all-matches % others same-hanzi?* contains-defs?)]
    (filter (comp not empty? matching-entries) variants)))

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
  (juxt #(str/lower-case (str/join (:pinyin %))) :pinyin :traditional))

(defn sort-defs
  "Sort the definitions of a dictionary entry."
  [entry]
  (let [sorted-defs (sort (:definition entry))]
    (assoc entry :definition sorted-defs)))

(defn sort-classifiers
  [classifiers]
  (sort-by pinyin>case>character classifiers))

(defn digits->diacritics*
  "Convert (only) Pinyin+digit syllables to diacritics."
  [s]
  (if (pe/pinyin-block+digits? s)
    (p/digits->diacritics s)
    s))

(defn add-diacritics
  "Add diacritics to the Pinyin of an entry."
  [entry]
  (let [pinyin+diacritics (map digits->diacritics* (:pinyin entry))]
    (assoc entry :pinyin pinyin+diacritics)))

(defn prepare-entries
  "Sort a list of dictionary entries, including entry definitions.
  Then convert the tone digits to diacritics."
  [entries]
  (->> entries
       (map sort-defs)
       (sort-by pinyin>case>character)
       (map add-diacritics)
       (vec)))                                              ; allow fetching by index
