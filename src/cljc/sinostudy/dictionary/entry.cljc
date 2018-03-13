(ns sinostudy.dictionary.entry
  (:require [clojure.string :as str]
            [sinostudy.rim.core :as rim]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.eval :as pe]
            [sinostudy.pinyin.core :as p]))

;;;; MATCHING

(defn name?
  "Determine is a dictionary entry is a name entry (e.g. person or place name)."
  [entry]
  (when-let [first-letter (first (first (d/pinyin entry)))]
    #?(:clj  (Character/isUpperCase ^char first-letter)
       :cljs (not= first-letter (.toLowerCase first-letter)))))

(defn matches-pinyin
  "True if the Pinyin of both entries is equal, disregarding case."
  [p1 p2]
  (let [lower-case= #(= (str/lower-case %1) (str/lower-case %2))]
    (every? true? (map lower-case= p1 p2))))

(defn matches
  "True if everything matches except the definitions and the Pinyin case."
  [e1 e2]
  (and (= (d/simp e1) (d/simp e2))
       (= (d/trad e1) (d/trad e2))
       (matches-pinyin (d/pinyin e1) (d/pinyin e2))))

;;;; POST-PROCESSING (CLIENT-SIDE)

(defn sort-defs
  "Sort the definitions of a dictionary entry."
  [entry]
  (let [sorted-defs (sort (d/defs entry))]
    (assoc entry d/defs sorted-defs)))

(def pinyin>case>character
  "Order first by Pinyin, then by case, then by character."
  (juxt #(str/lower-case (str/join (d/pinyin %))) d/pinyin d/trad))

(defn add-diacritics
  "Add diacritics to the Pinyin of an entry."
  [entry]
  (let [digits->diacritics* (fn [s]
                              (if (pe/pinyin-block+digits? s)
                                (p/digits->diacritics s)
                                s))]
    (let [pinyin+diacritics (map digits->diacritics* (d/pinyin entry))]
      (assoc entry d/pinyin pinyin+diacritics))))

(defn prepare-entries
  "Sort a list of dictionary entries, including entry definitions.
  Then convert the tone digits to diacritics."
  [entries]
  (->> entries
       (map sort-defs)
       (sort-by pinyin>case>character)
       (map add-diacritics)
       (vec)))                                              ; allow fetching by index

;; TODO: 唎 in traditional special case
(defn variant-def?
  "Is this definition a reference to a common char/word it is a variant of?"
  [definition]
  (not (nil? (re-find #"variant of" definition))))

(defn variant-entry?
  "Is this entry a variant of a more common char/word?"
  [entry]
  (let [definitions (d/defs entry)]
    (some variant-def? definitions)))

(defn same-hanzi?
  "Does the script (:simplified or :traditional) of entry match variant-entry?"
  [script variant-entry entry]
  (= (get variant-entry script) (get entry script)))

(defn contains-defs?
  "Does entry contain the definitions of variant-entry?"
  [variant-entry entry]
  (let [rest-defs (partial filter (complement variant-def?))
        e1-defs   (rest-defs (d/defs variant-entry))
        e2-defs   (set (d/defs entry))]
    (every? (partial contains? e2-defs) e1-defs)))

(defn false-variants
  "Find false variants (usually an artifact of using Simplified Chinese).
  A false variant is a hanzi variation that only exists in the other script."
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
