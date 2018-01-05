(ns sinostudy.dictionary.common
  (:require [clojure.string :as str]
            [sinostudy.pinyin.core :as pinyin]
            [sinostudy.pinyin.patterns :as patterns]))


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
  (if (patterns/pinyin-block+digits? s)
    (pinyin/digits->diacritics s)
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
