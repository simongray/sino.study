(ns user
  (:require [clojure.java.io :as io]
            [mount.core :as mount :refer [start stop]]
            [mount-up.core :as mount-up]
            [computerese.core :as c]
            [computerese.annotations :as ca]
            [computerese.semgraph.core :as sg]
            [computerese.loom.io :refer [view]]
            [sinostudy.handler :as handler]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.load :as load]
            [sinostudy.handler :as handler :refer [dict config server]]
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

(comment
  (def nlp (c/pipeline {:annotators "tokenize,ssplit,pos,depparse",
                        :depparse   {:model "edu/stanford/nlp/models/parser/nndep/UD_Chinese.gz"},
                        :ndepparse  {:language "chinese"},
                        :tokenize   {:language "zh"},
                        :segment    {:model                "edu/stanford/nlp/models/segmenter/chinese/ctb.gz",
                                     :sighanCorporaDict    "edu/stanford/nlp/models/segmenter/chinese",
                                     :serDictionary        "edu/stanford/nlp/models/segmenter/chinese/dict-chris6.ser.gz",
                                     :sighanPostProcessing "true"},
                        :ssplit     {:boundaryTokenRegex "[.。]|[!?！？]+"},
                        :pos        {:model "edu/stanford/nlp/models/pos-tagger/chinese-distsim/chinese-distsim.tagger"}})))

