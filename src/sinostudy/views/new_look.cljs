(ns sinostudy.views.new-look
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [sinostudy.db :as db]
            [sinostudy.subs :as subs]
            [sinostudy.events.core :as events]
            [sinostudy.events.scrolling :as scrolling]
            [sinostudy.events.actions :as actions]
            [sinostudy.views.dictionary :as vd]
            [sinostudy.navigation.pages :as pages]))

(defn title-input
  []
  (let [input           @(rf/subscribe [::subs/input])
        unknown-queries @(rf/subscribe [::subs/unknown-queries])
        actions         @(rf/subscribe [::subs/actions])
        unknown-query?  (when input
                          (contains? unknown-queries (str/trim input)))
        disabled?       (not (nil? actions))
        change-title    (fn [e]
                          (when (nil? actions)
                            (let [v (.-value (.-target e))]
                              (rf/dispatch [::events/on-input-change v]))))]
    [:input.titleInput {:type            "text"
                        :placeholder     "look up..."
                        :on-change       change-title
                        :value           input
                        :pattern         (when unknown-query?
                                           (str "^(?!" input ")$"))
                        :disabled        disabled?
                        :auto-capitalize "off"
                        :auto-correct    "off"
                        :auto-complete   "off"
                        :spell-check     false}]))

(defn app []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/current-nav]))]
    [:<>
     [title-input]]))
;[action-chooser]
;[header not-home?]
;[main]
;[footer]
;[version-digest (when not-home? {:class "hidden"})]]))
