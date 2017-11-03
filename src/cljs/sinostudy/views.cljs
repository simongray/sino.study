(ns sinostudy.views
  (:require [re-frame.core :as re-frame]
            [sinostudy.subs :as subs]))

;; https://purelyfunctional.tv/guide/reagent/
;; https://purelyfunctional.tv/guide/re-frame-building-blocks/
;; https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components
;; http://reagent-project.github.io/

;; https://www.w3schools.com/tags/tag_input.asp
(defn study-input []
  [:input#study-input {:type :text}])

(defn study-button []
  (let [label (re-frame/subscribe [::subs/study-button-label])]
    [:button#study-button
     {:type :submit
      :on-click (fn [e]
                  (.preventDefault e)
                  (re-frame/dispatch [:study]))}
     @label]))

(defn study-form []
  [:form#study-form
   [study-input]
   [study-button]])

(defn main-panel []
  [:div
   [study-form]])

