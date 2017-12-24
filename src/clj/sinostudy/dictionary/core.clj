(ns sinostudy.dictionary.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn entry?
  "Determining if a line is a dictionary entry."
  [line]
  (not (str/starts-with? line "#")))

(defn split-def
  "Splits the definition string into separate parts."
  [definition]
  (set (str/split definition #"/")))

(defn add-entry
  "Add (or extend) an entry in the dictionary; n marks the look-up key."
  [n dictionary [_ trad simp pinyin definition :as entry]]
  (let [key   (nth entry n)
        entry {:traditional trad
               :simplified  simp
               :pinyin      pinyin
               :definition  (split-def definition)}]
    (if-let [entries (get dictionary key)]
      (assoc dictionary key (conj entries entry))
      (assoc dictionary key #{entry}))))

;; compiles a vector of 3 dictionary maps with different look-up keys:
;; traditional, simplified, and pinyin
(def add-entries
  (let [add-trad   (partial add-entry 1)
        add-simp   (partial add-entry 2)
        add-pinyin (partial add-entry 3)]
    (juxt (partial reduce add-trad {})
          (partial reduce add-simp {})
          (partial reduce add-pinyin {}))))

(defn extract-entry
  "Extract the constituents of a matching dictionary entry according to regex:
  TRADITIONAL SIMPLIFIED [PINYIN] /DEFINITION/"
  [line]
  (re-matches #"^([^ ]+) ([^ ]+) \[([^]]+)\] /(.+)/" line))

(defn load-dictionaries
  "Load the contents of a CC-CEDICT dictionary file into a 3 dictionary maps:
  traditional, simplified, and Pinyin."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter entry?)
         (map extract-entry)
         add-entries)))
