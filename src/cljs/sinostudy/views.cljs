(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [sinostudy.site :as site]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]))

(defn input-field []
  (let [input (rf/subscribe [::subs/input])
        css-class (rf/subscribe [::subs/input-css-class])]
    [:input#study-input
     {:type        :text
      :value       @input
      :class       @css-class
      :on-change   (fn [e]
                     (rf/dispatch [::events/on-input-change
                                   (-> e .-target .-value)]))}]))

(defn input-button []
  (let [label (rf/subscribe [::subs/button-label])
        input (rf/subscribe [::subs/input])]
    [:button#study-button
     {:type :submit
      :on-click (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [::events/on-study-button-press @input]))}
     @label]))

(defn form []
  [:form#study-form
   [input-field]
   [input-button]])

(defn hint []
  (let [hint-key (rf/subscribe [::subs/hint-key])
        hint-content (rf/subscribe [::subs/hint-content])]
    [:div#study-hint
     {:key @hint-key}
     @hint-content]))

(defn header []
  [:div
   [site/header]
   [form]
   [hint]])

(defn page []
  (let [page? (rf/subscribe [::subs/page?])
        page-content (rf/subscribe [::subs/page-content])]
    (when @page?
      [:article
       @page-content])))

(def footer
  (site/footer "/"))

(defn main-panel []
  (let [page? (rf/subscribe [::subs/page?])]
    [:div#bg
     [:div {:class (if @page? "main top" "main")}
      [:div#aligner
       [header]]]
     [page]
     [footer]]))
