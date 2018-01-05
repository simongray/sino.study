(ns sinostudy.handler
  (:import (java.io ByteArrayOutputStream))
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [cognitect.transit :as transit]
            [sinostudy.dictionary.core :as dict]))

;; TODO: figure out how to split into dev/production
;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/server.md#cross-origin-requests

;; TODO: use coercions for regex check of input
;; https://weavejester.github.io/compojure/compojure.coercions.html

(def reframe-app
  (slurp "./resources/public/index.html"))

(defonce dictionaries
  (dict/load-dictionaries "./resources/cedict_ts.u8"))

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
  to a base directory (query-type) and content (query-string)."
  [query-type query-string]
  (case query-type
    "word" (dict/look-up query-string dictionaries)))

(defn query-result
  "Get the Transit-encoded result of a query."
  [query-type query-string]
  (let [result (execute-query query-type query-string)
        page-category (keyword query-type)]
    (transit-write {:page [page-category query-string]
                    :result result})))

(defroutes app-routes
  ;; HTML pages all resolve to the ClojureScript app.
  ;; The internal routing of the app then provides the correct presentation.
  (GET "/" [] reframe-app)
  (GET "/help" [] reframe-app)
  (GET "/blog" [] reframe-app)
  (GET "/about" [] reframe-app)
  (GET "/word/:word" [] reframe-app)
  (GET "/word/:word/:n" [] reframe-app)

  ;; ANY rather than GET is necessary to allow cross origin requests during dev.
  (ANY "/query/:query-type/:query-string" [query-type query-string]
    {:status  200
     :headers ajax-headers
     :body    (query-result query-type query-string)})

  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
