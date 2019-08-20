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
  "The combined page title + text input field. Pages are dynamically loaded when
  the user edits this field."
  []
  (let [input           @(rf/subscribe [::subs/input])
        unknown-queries @(rf/subscribe [::subs/unknown-queries])
        actions         @(rf/subscribe [::subs/actions])
        id              @(rf/subscribe [::subs/current-id])
        unknown-query?  (when input
                          (contains? unknown-queries (str/trim input)))
        disabled?       (not (nil? actions))
        change-title    (fn [e]
                          (when (nil? actions)
                            (let [v (.-value (.-target e))]
                              (rf/dispatch [::events/on-input-change v]))))]
    [:input#titleInput {:type            "text"
                        :placeholder     (if (= id "/")
                                           "look up..."
                                           id)
                        :on-change       change-title
                        :value           input

                        ;; This will mark the input as :invalid when the query
                        ;; doesn't return a result from the backend. We simply
                        ;; check for the existence of the query in the set of
                        ;; unknown queries and use the built-in pattern attr.
                        ;; to match anything BUT the current val if applicable.
                        :pattern         (when unknown-query?
                                           (str "^(?!" input ")$"))

                        :disabled        disabled?
                        :auto-capitalize "off"
                        :auto-correct    "off"
                        :auto-complete   "off"
                        :spell-check     false}]))

(defn page-content
  "The ever-changing content pane of the site. Encompasses both dynamic
  dictionary look-ups as well as static page content."
  []
  (let [page-type       @(rf/subscribe [::subs/current-page-type])
        input           @(rf/subscribe [::subs/input])
        unknown-queries @(rf/subscribe [::subs/unknown-queries])
        unknown-query?  (when input
                          (contains? unknown-queries (str/trim input)))]
    [:main.pageContent {:class (when unknown-query?
                                 "pageContent--unknownTerm")}
     page-type]))

(defn app []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/current-nav]))]
    [:<>
     [title-input]
     [page-content]]))
;[action-chooser]
;[header not-home?]
;[main]
;[footer]
;[version-digest (when not-home? {:class "hidden"})]]))
