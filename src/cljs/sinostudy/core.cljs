(ns sinostudy.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-frame.db :as db]
            [day8.re-frame.http-fx]
            [secretary.core :as secretary]
            [sinostudy.subs :as subs]
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
    (secretary/dispatch! current-page))

  ;; The input bar needs to have immediate focus on page load.
  (.focus (.getElementById js/document "study-input"))

  ;; Intercepts all key presses in the document.
  ;; Only defers from normal operation in the action-chooser mode.
  ;; This is important, since calling .preventDefault on all key presses
  ;; is a recipe for creating many bugs -- now and down the line, too.
  (set! (.-onkeydown js/document)
        (fn [e] (when-let [actions @(rf/subscribe [::subs/actions])]
                  (.preventDefault e)
                  (rf/dispatch [::events/on-key-down (.-key e)])))))

(defn ^:export init []
  (routes/app-routes)
  (rf/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
