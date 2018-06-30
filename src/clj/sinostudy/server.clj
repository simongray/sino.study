(ns sinostudy.server
  (:require [org.httpkit.server :as hs]
            [sinostudy.handler :as handler])
  (:gen-class))

(defn -main []
  (let [port 8080]
    (hs/run-server #'handler/app {:port port})
    (println (str "Listening on port " port))))
