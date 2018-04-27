(ns user
  (:require [clojure.java.io :as io]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.load :as load]
            [sinostudy.pinyin.core :as p]))

(defn new-dict
  []
  (let [listings     (load/load-cedict
                       (io/resource "cedict_ts.u8"))
        freq-dict    (load/load-freq-dict
                       (io/resource "frequency/internet-zh.num.txt")
                       (io/resource "frequency/giga-zh.num.txt"))
        makemeahanzi (load/load-makemeahanzi
                       (io/resource "makemeahanzi/dictionary.txt"))]
    (d/create-dict listings freq-dict makemeahanzi)))
