(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]))

(defn header []
  [:header
   [:div#logo]])

(defn study-input []
  (let [input (rf/subscribe [::subs/input])
        input-placeholder (rf/subscribe [::subs/input-placeholder])
        evaluation (rf/subscribe [::subs/evaluation])]
    [:input#study-input
     {:type :text
      :value @input
      :placeholder @input-placeholder
      :class @evaluation
      :on-change (fn [e]
                   (rf/dispatch [::events/input-change (-> e
                                                           .-target
                                                           .-value)]))}]))

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

(defn study-hint []
  (let [hint-key (rf/subscribe [::subs/hint-key])
        hint-content (rf/subscribe [::subs/hint-content])]
    [:div#study-hint
     {:key @hint-key}
     @hint-content]))

(defn study-history []
  (let [queries (rf/subscribe [::subs/queries])]
    [:ul#card-list
     (for [query @queries]
       [:li.card
        {:key (:id query)
         :class (when (= (:state query) :failure) "query-failure")}
        (:content query)])]))

(defn footer []
  [:footer
   [:p
    [:a {:href "/help"} "Help"]
    " · "
    [:a {:href "/about"} "About"]
    " · "
    [:a {:href "/blog"} "Blog"]
    " · © 2018 Simon Gray"]])

(defn main-panel []
  [:div#page
   [header]
   [study-form]
   [study-hint]
   [study-history]
   [footer]])
