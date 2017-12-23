(ns sinostudy.dictionary.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn entry?
  "Determining if a line is a dictionary entry."
  [line]
  (not (str/starts-with? line "#")))

(defn add-entry
  "Add (or update) an entry in the dictionary."
  [dictionary [_ simp trad pinyin definition]]
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

(def dictionary
  (with-open [reader (io/reader "cedict_ts.u8")]
    (->> (line-seq reader)
         (filter entry?)
         (map extract-entry)
         (reduce add-entry {}))))

;; testing...
(doseq [entry (take 30 (seq dictionary))]
  (println entry))
