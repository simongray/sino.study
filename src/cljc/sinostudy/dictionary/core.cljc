(ns sinostudy.dictionary.core
  (:require [clojure.set :as set]))

;; TODO: Beijing returns 背景, not list of entries
;; TODO: dual entries:
;;       帆 fān Taiwan pr. [fan2]; to gallop; variant of 帆
;;       帆 fān Taiwan pr. [fan2], except 帆布; sail
;; TODO: merge entries that only differ in definitions, trad and simp may differ
;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable
;; TODO: make the pattern "classifier for X" prominent (CL)
;; TODO: make the pattern "to X" prominent (V)
;; TODO: do something about weird simplifications like 制, 和 (dual entries in simp)
;;       perhaps simply merge while constructing the list and remove "variant
;;       of X" from definitions?
;; TODO: merge duoyinci, perhaps just conj Pinyin and defs
;;       e.g. {... :pinyin [[...] [...]], :definition [[...] [...]]}
;; TODO: tag radicals, e.g. def = "Kangxi radical 206" or just from a list

(def defs
  :definitions)

(def pinyin
  :pinyin)

(def pinyin-key
  :pinyin-key)

(def pinyin+digits-key
  :pinyin+digits-key)

(def pinyin+diacritics-key
  :pinyin+diacritics-key)

(def simp
  :simplified)

(def trad
  :traditional)

(def cls
  :classifiers)

(def vars
  :variations)

(def uses
  :uses)

(def scripts
  :scripts)

(defn look-up
  "Look up the specified word in each dictionary map and merge the results."
  [word dicts]
  (let [check-dict (fn [n] (get (nth (vals dicts) n) word))]
    (->> (map check-dict (range (count dicts)))
         (filter (comp not nil?))
         (apply set/union))))
