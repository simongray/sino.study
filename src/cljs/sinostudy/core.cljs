(ns sinostudy.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [sinostudy.routes :as routes]
            [sinostudy.events :as events]
            [sinostudy.views :as views]
            [sinostudy.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app"))
  ;; Start the CLJS app from current page in the address bar.
  ;; The routing mostly takes place on the frontend,
  ;; so the app needs to orient itself on hard page loads.
  (let [current-page (-> js/window .-location .-pathname)]
    (when (not= "/" current-page)
      (rf/dispatch [::events/change-page [:static current-page]])))
  (.focus (.getElementById js/document "study-input")))

(defn ^:export init []
  (routes/app-routes)
  (rf/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
