(ns sinostudy.dictionary.loader
  (:require [clojure.java.io :as io]
            [sinostudy.dictionary.core :as dict]))

(defn load-entries
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [file]
  (with-open [reader (io/reader file)]
    (->> (line-seq reader)
         (filter dict/entry?)
         (map dict/extract-entry)
         (vec))))
