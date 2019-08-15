(ns sinostudy.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [secretary.core :as secretary]
            [sinostudy.navigation.routes :as routes]
            [sinostudy.state.events.core :as events]
            [sinostudy.state.subs :as subs]
            [sinostudy.views.core :as views]
            [sinostudy.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/app] (.getElementById js/document "app"))

  ;; Start the CLJS app from current page in the address bar.
  ;; The routing mostly takes place on the frontend,
  ;; so the app needs to orient itself on hard page loads.
  (let [current-page (-> js/window .-location .-pathname)]
    (secretary/dispatch! current-page))

  ;; The input bar needs to have immediate focus on page load.
  (.focus (.getElementById js/document "input-field"))

  ;; Intercepts all key presses in the document.
  ;; Only defers from normal operation in the action-chooser mode.
  ;; This is important, since calling .preventDefault on all key presses
  ;; is a recipe for creating many bugs -- now and down the line, too.
  (set! (.-onkeydown js/document)
        (fn [e] (when @(rf/subscribe [::subs/actions])
                  (.preventDefault e)
                  (rf/dispatch [::events/on-key-down (.-key e)])))))

(defn ^:export init []
  (routes/app-routes)
  (rf/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
