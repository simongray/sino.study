(ns user
  "Sino.study uses mount for managing state:

    ;; Start a new system
    (mount/start)

    ;; Stop the running system
    (mount/stop)"
  (:require [clojure.java.io :as io]
            [mount.core :as mount]
            [mount-up.core :as mount-up]
            [sinostudy.handler :as handler]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.load :as load]
            [sinostudy.handler :as handler :refer [dict config server]]
            [sinostudy.pinyin.core :as p]))

(mount-up/on-upndown :info mount-up/log :before)

(defn look-up*
  "A version of look-up that performs both the backend and frontend processing.
  Useful for testing what the search results on the frontend look like."
  [term]
  (->> term
       (d/look-up dict)
       (d/reduce-result)
       (d/sort-result)))
