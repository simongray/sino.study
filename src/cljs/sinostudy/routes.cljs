(ns sinostudy.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [goog.events :as gevents]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re-frame]
            [sinostudy.events :as events]
            [accountant.core :as accountant]))

;; as defined in Day8/re-frame-template
(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  ;; this prefixes routes with a hash for compability with older browsers
  ;; however, it might not be necessary if I don't need to support IE 9
  ;; furthermore, it may impede on some other functionality
  (secretary/set-config! :prefix "#")

  ;; Combining the root route with the other page routes don't seem to work.
  (defroute "/" []
    (re-frame/dispatch [::events/change-page [:static "/"]]))

  (defroute "/:page" [page]
    (re-frame/dispatch [::events/change-page [:static (str "/" page)]]))

  (defroute
    "/word/:word" [word]
    (re-frame/dispatch [::events/change-page [:word word]]))

  (hook-browser-navigation!)

  ;; following instructions at https://github.com/venantius/accountant
  (accountant/configure-navigation!
    {:nav-handler  (fn [path] (secretary/dispatch! path))
     :path-exists? (fn [path] (secretary/locate-route path))}))
