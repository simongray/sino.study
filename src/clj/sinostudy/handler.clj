(ns sinostudy.handler
  (:import (java.io ByteArrayOutputStream))
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [cognitect.transit :as transit]
            [sinostudy.pages.defaults :as pd]
            [sinostudy.dictionary.defaults :as dd]
            [sinostudy.dictionary.loader :as loader]
            [sinostudy.dictionary.core :as dict]))

;; TODO: split into dev/production
;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/server.md#cross-origin-requests

;; TODO: use coercions for regex check of input
;; https://weavejester.github.io/compojure/compojure.coercions.html

(def dictionary-keys
  [dd/trad
   dd/simp
   dd/pinyin-key
   dd/pinyin+digits-key
   dd/pinyin+diacritics-key])

(def index.html
  (slurp "./resources/public/index.html"))

(defonce dicts
  (let [entries (loader/load-entries "./resources/cedict_ts.u8")]
    (dict/load-dicts entries dictionary-keys)))

;; First Access-Control header permits cross-origin requests.
;; Second prevents Chrome from stripping Content-Type header.
(def ajax-headers
  {"Access-Control-Allow-Origin"  "*"
   "Access-Control-Allow-Headers" "Content-Type"
   "Content-Type"                 "application/transit+json; charset=utf-8"})

(defn transit-write [x]
  "Encode Clojure data using Transit (adapted from David Nolen's example)."
  (let [baos (ByteArrayOutputStream.)
        w    (transit/writer baos :json)
        _    (transit/write w x)
        ret  (.toString baos)]
    (.reset baos)
    ret))

(defn execute-query
  "Execute a query from the ClojureScript app. The queries all resolve
  to a page-type (keyword) and a query-string."
  [page-type query-string]
  (cond
    (= pd/words page-type) (dict/look-up query-string dicts)))

(defn query-result
  "Get the Transit-encoded result of a query."
  [query-type query-string]
  (let [page-type (keyword query-type)
        result    (execute-query page-type query-string)]
    (transit-write {:page   [page-type query-string]
                    :result result})))

(defroutes app-routes
  ;; ANY rather than GET is necessary to allow cross origin requests during dev.
  (ANY "/query/:query-type/:query-string" [query-type query-string]
    {:status  200
     :headers ajax-headers
     :body    (query-result query-type query-string)})

  ;; HTML page requests all resolve to the ClojureScript app.
  ;; The internal routing of the app creates the correct presentation.
  (route/not-found index.html))

(def app
  (wrap-defaults app-routes site-defaults))
