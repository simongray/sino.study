(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]))

;; https://purelyfunctional.tv/guide/reagent/
;; https://purelyfunctional.tv/guide/re-frame-building-blocks/
;; https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components
;; http://reagent-project.github.io/

;; https://www.w3schools.com/tags/tag_input.asp

;; EXAMPLES
;; https://github.com/Day8/re-frame/blob/master/examples/simple/src/simple/core.cljs
;  http://timothypratley.github.io/reanimated/#!/examples.core

(defn logo []
  [:h1#logo "sino·study"])

(defn study-input []
  (let [input (rf/subscribe [::subs/input])
        evaluation (rf/subscribe [::subs/evaluation])]
    [:input#study-input
     {:type :text
      :value @input
      :class @evaluation
      :on-change (fn [e]
                   (rf/dispatch [::events/input-change (-> e .-target .-value)]))}]))

(defn study-button []
  (let [label (rf/subscribe [::subs/button-label])
        input (rf/subscribe [::subs/input])]
    [:button#study-button
     {:type :submit
      :on-click (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [::events/query @input]))}
     @label]))

(defn study-form []
  [:form#study-form
   [study-input]
   [study-button]])

(defn study-history []
  (let [queries (rf/subscribe [::subs/queries])]
    [:ul#card-list
     (for [query @queries]
       [:li.card {:key (:id query)} (:content query)])]))

(defn main-panel []
  [:div#page
   [logo]
   [study-form]
   [study-history]])
