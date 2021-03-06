(ns sinostudy.navigation.handler
  (:import [java.io ByteArrayOutputStream])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.reader :as reader]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [cognitect.transit :as transit]
            [org.httpkit.server :as hs]
            [mount.core :as mount :refer [defstate]]
            [mount-up.core :as mount-up]
            [sinostudy.navigation.pages :as pages]
            [sinostudy.dictionary.load :as dl]
            [sinostudy.dictionary.core :as d])
  (:gen-class))

;; TODO: split into dev/production
;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/server.md#cross-origin-requests

;; TODO: use coercions for regex check of input
;; https://weavejester.github.io/compojure/compojure.coercions.html

(defstate config
  "System config file (EDN format)."
  :start (-> "config.edn" io/resource slurp reader/read-string))

(defstate dict
  "Dictionary used for Chinese/English/pinyin term look-ups."
  :start (dl/load-dict))

(def index
  (slurp (io/resource "public/index.html")))

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
  [type query opts]
  (cond
    (= ::pages/terms type) (d/look-up dict query)))

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
     :body    (transit-result (keyword (str 'sinostudy.navigation.pages) type)
                              query
                              opts)})

  ;; HTML page requests all resolve to the ClojureScript app.
  ;; The internal routing of the app creates the correct presentation.
  (ANY "*" [] index))

;; Allows web resources in the JAR (such as CSS and JS) to be fetched.
;; This is especially important in production, i.e. using html-kit.
;; Otherwise, the paths referencing them in index.html will return nothing.
(defroutes resources-routes
  (route/resources "/" {:root "public"}))

(def all-routes
  (routes resources-routes
          app-routes))

(def app
  (wrap-defaults all-routes site-defaults))

(defstate server
  "Server instance (http-kit)."
  :start (hs/run-server #'app {:port (get-in config [:server :port :internal] 8080)})
  :stop (server))

(defn -main
  []
  (mount-up/on-upndown :info mount-up/log :before)
  (mount/start)
  (println (str "Listening on port " (get-in config [:server :port :internal] 8080))))
