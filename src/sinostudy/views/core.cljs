(ns sinostudy.views.core
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [sinostudy.db :as db]
            [sinostudy.subs :as subs]
            [sinostudy.events.core :as events]
            [sinostudy.events.scrolling :as scrolling]
            [sinostudy.events.actions :as actions]
            [sinostudy.views.actions :as va]
            [sinostudy.views.dictionary :as vd]
            [sinostudy.navigation.pages :as pages])
  (:require-macros [sinostudy.macros.core :as macros]))

;;;; HELPER FUNCTIONS

(defn navlink
  [from to text]
  (let [key (str from "->" to)]
    (if (= from to)
      [:a.current-page
       {:key key}
       text]
      [:a
       {:on-click #(rf/dispatch [::scrolling/reset-scroll-state
                                 [::pages/static to]])
        :href     to
        :key      key}
       text])))

(defn navify [from links]
  (map (fn [[to text]] (navlink from to text)) links))


;;;; VIEWS

(defn smart-input []
  "The input field (part of the header form)."
  (let [input           @(rf/subscribe [::subs/input])
        actions         @(rf/subscribe [::subs/actions])
        unknown-queries @(rf/subscribe [::subs/unknown-queries])
        disabled?       (not (nil? actions))
        unknown-query?  (when input
                          (contains? unknown-queries (str/trim input)))]
    [:<>
     [:div#header-input
      [:input#input-field
       {:type            "text"
        :class           (when unknown-query? "unknown")
        :placeholder     "look up..."
        :auto-capitalize "off"
        :auto-correct    "off"
        :auto-complete   "off"
        :spell-check     false
        :disabled        disabled?
        :value           input
        :on-change       (fn [e]
                           (when (nil? actions)
                             (rf/dispatch [::events/on-input-change
                                           (-> e .-target .-value)])))}]

      ;; The button is not actually displayed!
      ;; It's kept around to prevent "Enter" submitting the input to an unknown href.
      ;; If the button isn't there, pressing enter to select an action in the
      ;; action-chooser can misfire a submit event. The on-click event in the submit
      ;; button captures these submit events and sends straight them to /dev/null.
      [:button
       {:type     "submit"
        :on-click (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [::events/submit input]))}
       "go"]]]))

(defn filters
  "Filter for what type of dictionary search result should be shown."
  []
  (let [{search-term :term} @(rf/subscribe [::subs/content])
        current-filter @(rf/subscribe [::subs/current-result-filter])
        result-types   @(rf/subscribe [::subs/current-result-types])
        hidden?        (not (and result-types
                                 (> (count result-types) 1)))]
    [:div#filters
     {:class (when hidden? "hidden")}
     (interpose " · "
       (for [result-type result-types]
         (let [result-type-str (str/capitalize (name result-type))]
           [:span {:key result-type}
            [:input {:type      "radio"
                     :name      "result-filter"
                     :value     result-type
                     :id        result-type
                     :checked   (= current-filter result-type)
                     :on-change (fn [_]
                                  (rf/dispatch [::events/set-result-filter
                                                search-term
                                                result-type]))}]
            [:label {:for   result-type
                     :title (str "View " result-type-str " results")}
             result-type-str]])))]))

(defn header
  "The header contains the logo and the main input form."
  []
  (let [page  @(rf/subscribe [::subs/current-page])
        input @(rf/subscribe [::subs/input])]
    [:header
     [:div#aligner
      [:form {:auto-complete "off"}
       [smart-input]
       (when-let [title (and (not= input (second page))
                             (events/mk-input page))]
         [:p#title "↓ " [:em title] " ↓"])
       [filters]]]]))

(defn main
  "The content pane of the site."
  []
  (reagent/create-class
    {:display-name
     "main"

     :reagent-render
     (fn []
       (let [category @(rf/subscribe [::subs/current-category])
             content  @(rf/subscribe [::subs/content])]
         (cond
           (= ::pages/static category) (or content (db/static-pages "/404"))
           (= ::pages/terms category) [vd/dictionary-page])))

     ;; Ensures that scroll state is restored when pushing back/forward button.
     ;; Sadly, this behaviour is global for all updates, so links/buttons/etc.
     ;; must manually dispatch ::scrolling/reset-scroll-state to avoid this!
     :component-did-update
     (fn [_ _]
       (let [page @(rf/subscribe [::subs/current-page])]
         (rf/dispatch [::scrolling/load-scroll-state page])))}))

(defn script-changer []
  "The button used to toggle traditional/simplified Chinese script."
  (let [script     @(rf/subscribe [::subs/script])
        text       (if (= :simplified script)
                     "Simpl."
                     "Trad.")
        alt-script (if (= :simplified script)
                     :traditional
                     :simplified)
        title      (str "Click to use " (if (= :simplified alt-script)
                                          "simplified characters"
                                          "traditional characters"))]
    [:a#script-changer
     {:key      alt-script
      :title    title
      :on-click #(rf/dispatch [::events/change-script alt-script])}
     text]))

(defn footer []
  "The footer (contains navigation)."
  (let [from  @(rf/subscribe [::subs/current-nav])
        links [["/" "Home"] ["/about" "About"] ["/settings" "Settings"]]]
    [:footer
     [:nav (interpose " · "
             (conj (vec (navify from links))
                   [script-changer {:key "script-changer"}]))]]))

;;; Project version based on git tag
;;; See: https://github.com/arrdem/lein-git-version
(defn version-digest
  "Current version with link to project on Github."
  [attr]
  (let [version (reader/read-string (macros/slurp "resources/version.edn"))]
    [:address attr
     [:a {:href "https://github.com/simongray/sino.study"}
      (:tag version)]]))

(defn app []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/current-nav]))]
    [:<>
     [va/action-chooser]
     [header not-home?]
     [main]
     [footer]
     [version-digest (when not-home? {:class "hidden"})]]))
