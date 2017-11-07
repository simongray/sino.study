(ns sinostudy.views
  (:require [re-frame.core :as re-frame]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]))

;; https://purelyfunctional.tv/guide/reagent/
;; https://purelyfunctional.tv/guide/re-frame-building-blocks/
;; https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components
;; http://reagent-project.github.io/

;; https://www.w3schools.com/tags/tag_input.asp

;; EXAMPLES
;; https://github.com/Day8/re-frame/blob/master/examples/simple/src/simple/core.cljs

(defn click-study-button
  [e]
  (.preventDefault e)
  (re-frame/dispatch [::events/study]))

(defn change-study-input
  [e]
  (re-frame/dispatch [::events/input-change (-> e .-target .-value)]))

(defn study-input []
  (let [input (re-frame/subscribe [::subs/input])]
    [:input#study-input
     {:type      :text
      :value     @input
      :on-change change-study-input}]))

(defn study-button []
  (let [label (re-frame/subscribe [::subs/button-label])]
    [:button#study-button
     {:type :submit
      :on-click click-study-button}
     @label]))

(defn study-form []
  [:form#study-form
   [study-input]
   [study-button]])

(defn main-panel []
  [:div
   [study-form]])
