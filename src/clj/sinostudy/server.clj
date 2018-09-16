(ns sinostudy.server
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as hs]
            [sinostudy.handler :as handler]
            [clojure.tools.reader :as reader])
  (:gen-class))

(def server (atom nil))

(def config
  (reader/read-string (slurp (io/resource "config.edn"))))

(defn stop-server!
  "Stop a running http-kit server."
  []
  (when-let [stop-server @server]
    (stop-server)))

(defn start-server!
  "Start a production web server using http-kit."
  ([port]
   (stop-server!)
   (reset! server (hs/run-server #'handler/app {:port port})))
  ([]
   (start-server! (:port config))))

(defn -main
  []
  (println "Loading dict...")
  (handler/load-dict!)
  (println "Starting server...")
  (start-server!)
  (println (str "Listening on port " (:port config))))
