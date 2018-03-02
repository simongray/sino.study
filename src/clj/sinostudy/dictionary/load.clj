(ns sinostudy.dictionary.load
  (:require [clojure.java.io :as io]
            [sinostudy.dictionary.entry :as entry]))

(defn listings
  "Load the listings of a CC-CEDICT dictionary file into Clojure maps."
  [file-path]
  (with-open [reader (io/reader file-path)]
    (->> (line-seq reader)
         (filter entry/entry?)
         (map entry/line->listing)
         (vec))))
