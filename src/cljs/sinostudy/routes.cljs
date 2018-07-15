(ns sinostudy.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [re-frame.core :as re-frame]
            [sinostudy.events :as events]
            [sinostudy.pages.core :as pages]
            [accountant.core :as accountant]))

;; Since scroll restoration differs in implementation between e.g. Firefox
;; and Chrome -- and neither implementations are good enough -- the safest
;; choice is to carefully disable scroll restoration (default: "automatic").
(when (exists? js/window.history.scrollRestoration)
  (set! js/window.history.scrollRestoration "manual"))

(defn app-routes []
  ;; this prefixes routes with a hash for compability with older browsers
  ;; however, it might not be necessary if I don't need to support IE 9
  ;; furthermore, it may impede on some other functionality
  (secretary/set-config! :prefix "#")

  ;; Combining the root route with the other page routes don't seem to work.
  (defroute "/" []
    (re-frame/dispatch [::events/change-page [::pages/static "/"]]))

  (defroute "/:page" [page]
    (re-frame/dispatch [::events/change-page [::pages/static (str "/" page)]]))

  (defroute
    (str "/" (name :pages/terms) "/:term") [term]
    (re-frame/dispatch [::events/change-page [::pages/terms term]]))

  (defroute
    (str "/" (name ::pages/terms) "/:term/:attribute") [term attribute]
    (re-frame/dispatch [::events/change-page [::pages/terms term attribute]]))

  ;; following instructions at https://github.com/venantius/accountant
  (accountant/configure-navigation!
    {:nav-handler  (fn [path] (secretary/dispatch! path))
     :path-exists? (fn [path] (secretary/locate-route path))}))
