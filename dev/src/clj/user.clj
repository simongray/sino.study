(ns user
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as hs]
            [sinostudy.handler :as handler]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.load :as load]
            [sinostudy.pinyin.core :as p]
            [sinostudy.server :as server :refer [start-server! stop-server!]]
            [sinostudy.handler :refer [load-dict! dict]]
            [clojure.tools.reader :as reader]))

(def config
  (reader/read-string (slurp "resources/config.edn")))

(defn look-up*
  "A version of look-up that performs both the backend and frontend processing.
  Useful for testing what the search results on the frontend look like."
  [term]
  (->> term
       (d/look-up @dict)
       (d/reduce-result)
       (d/sort-result)))

(defn go
  "Load the dict and start a server."
  []
  (server/-main))
