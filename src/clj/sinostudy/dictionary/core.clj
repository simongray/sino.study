(ns sinostudy.dictionary.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn entry?
  "Determining if a line is a dictionary entry."
  [line]
  (not (str/starts-with? line "#")))

(defn add-entry
  "Add (or update) an entry in the dictionary."
  [dictionary [_ trad simp pinyin definition]]
  (if-let [entry (get dictionary simp)]
    (-> dictionary
        (assoc-in [simp :pinyin] (conj (:pinyin entry) pinyin))
        (assoc-in [simp :definition] (conj (:definition entry) definition)))
    (-> dictionary
        (assoc simp {:traditional trad
                     :pinyin      #{pinyin}
                     :definition  #{definition}}))))

(defn extract-entry
  "Extract the constituents of a matching dictionary entry."
  [line]
  ;; regex:   TRADITIONAL SIMPLIFIED [PINYIN] /DEFINITION/
  (re-matches #"^([^ ]+) ([^ ]+) \[([^]]+)\] /(.+)/" line))

(defn load-dictionary
  "Load the contents of a CC-CEDICT dictionary file into a map."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter entry?)
         (map extract-entry)
         (reduce add-entry {}))))
