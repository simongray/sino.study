(ns user
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [mount.core :as mount :refer [start stop]]
            [mount-up.core :as mount-up]
            [sinostudy.spec.entry :as entry]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.load :as load]
            [sinostudy.navigation.handler :as handler :refer [dict config server]]
            [sinostudy.pinyin.core :as p]))

(mount-up/on-upndown :info mount-up/log :before)

(defn restart
  "Restart one or more pieces of mount state."
  [& states]
  (apply stop states)
  (apply start states))

(defn look-up*
  "A version of look-up that performs both the backend and frontend processing.
  Useful for testing what the search results on the frontend look like."
  [term]
  (->> term
       (d/look-up dict)
       (d/reduce-result)
       (d/sort-result)))
