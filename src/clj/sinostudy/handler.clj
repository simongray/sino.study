(ns sinostudy.handler
  (:import (java.io ByteArrayOutputStream))
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [cognitect.transit :as transit]
            [sinostudy.pages.defaults :as pd]
            [sinostudy.dictionary.load :as load]
            [sinostudy.dictionary.core :as d]))

;; TODO: split into dev/production
;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/server.md#cross-origin-requests

;; TODO: use coercions for regex check of input
;; https://weavejester.github.io/compojure/compojure.coercions.html

(def index
  (slurp (io/resource "public/index.html")))

(defonce dicts
  (let [listings     (load/load-cedict
                       (io/resource "cedict_ts.u8"))
        freq-dict    (load/load-freq-dict
                       (io/resource "frequency/internet-zh.num.txt")
                       (io/resource "frequency/giga-zh.num.txt"))
        makemeahanzi (load/load-makemeahanzi
                       (io/resource "makemeahanzi/dictionary.txt"))]
    (d/create-dicts listings freq-dict makemeahanzi)))

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

(defn ns-keywords
  "Convert a string separated by a delimiter into namespaced keywords."
  [re ns s]
  (if (string? s)
    (->> (str/split s re)
         (map (partial keyword (str ns)))
         (set))
    s))

(defn execute-query
  "Execute a query from the ClojureScript app.
  The queries all resolve to a type, a query, and optional parameters."
  [type query {:keys [limit]}]
  (let [ns-keywords* (partial ns-keywords #"," 'sinostudy.dictionary.core)]
    (cond
      (= pd/terms type) (d/look-up dicts query (ns-keywords* limit)))))

(defn transit-result
  "Get the Transit-encoded result of a query."
  [type query opts]
  (transit-write {:page   [type query]
                  :result (execute-query type query opts)}))

(defroutes app-routes
  ;; ANY rather than GET is necessary to allow cross origin requests during dev.
  (ANY "/query/:type/:query" [type query & opts]
    {:status  200
     :headers ajax-headers
     :body    (transit-result (keyword type) query opts)})

  ;; HTML page requests all resolve to the ClojureScript app.
  ;; The internal routing of the app creates the correct presentation.
  (route/not-found index))

(def app
  (wrap-defaults app-routes site-defaults))
