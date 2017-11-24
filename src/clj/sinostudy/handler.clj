(ns sinostudy.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] "Hello World")

  ;; TODO: figure out how to split into dev/production
  ;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/server.md#cross-origin-requests
  (ANY "/query" []
    {:status 200
     :headers {"Access-Control-Allow-Origin" "*"
               "Access-Control-Allow-Headers" "Content-Type"}
     :body "hello re-frame!"})

  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
