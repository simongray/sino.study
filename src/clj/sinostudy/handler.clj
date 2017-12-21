(ns sinostudy.handler
  (:require [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] (io/as-file "./resources/public/index.html"))

  ;; TODO: use coercions for regex check of input
  ;; https://weavejester.github.io/compojure/compojure.coercions.html


  ;; TODO: figure out how to split into dev/production
  ;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/server.md#cross-origin-requests

  ;; "We must also change the request method from GET or POST to ANY.
  ;; The browser will actually submit two requests.
  ;; The first is an OPTIONS request submitted in order to probe the endpoint.
  ;; The second is the main GET or POST request."
  (ANY "/query" []
    {:status  200
     :headers {; header telling browser to permit cross-origin request
               "Access-Control-Allow-Origin"  "*"
               ; prevent Chrome stripping Content-Type header from our requests
               "Access-Control-Allow-Headers" "Content-Type"}
     :body    "hello re-frame!"})

  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
