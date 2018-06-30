(ns user
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as hs]
            [sinostudy.handler :as handler]
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

(defn look-up*
  "A version of look-up that performs both the backend and frontend processing.
  Useful for testing what the search results on the frontend look like."
  [dict term]
  (->> term
       (d/look-up dict)
       (d/reduce-result)
       (d/sort-result)))

(defn start-server
  "Start a production web server using http-kit.
  The returned function allows for stopping the server again.

    Usage: (def stop-server (start-server))"
  []
  (hs/run-server #'handler/app {:port 8080}))
