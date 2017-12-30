(ns sinostudy.dictionary.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [sinostudy.pinyin.core :as pinyin]))

(defn entry?
  "Determine if a line is a dictionary entry."
  [line]
  (not (str/starts-with? line "#")))

(defn split-def
  "Split the definition string into separate parts."
  [definition]
  (set (str/split definition #"/")))

(defn extract-entry
  "Extract the constituents of a matching dictionary entry according to regex:
  TRADITIONAL SIMPLIFIED [PINYIN] /DEFINITION/"
  [line]
  (let [pattern #"^([^ ]+) ([^ ]+) \[([^]]+)\] /(.+)/"
        [_ trad simp pinyin definition :as entry] (re-matches pattern line)]
    (when entry
      [trad simp (pinyin/pinyin-key pinyin) pinyin (split-def definition)])))

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
         (reduce conj))))
