(ns sinostudy.views
  (:require [re-frame.core :as re-frame]
            [sinostudy.subs :as subs]))


(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div#hello-message "Hello from " @name]))
