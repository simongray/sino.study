(ns sinostudy.dictionary.common
  (:require [clojure.string :as str]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]))

(def hanzi-ref
  "A pattern used in CC-CEDICT to embed a hanzi reference, e.g. 樁|桩[zhuang1]."
  #"[^,:\[a-zA-Z0-9]+\[[^\]]+\]+")

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
