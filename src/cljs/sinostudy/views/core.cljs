(ns sinostudy.views.core
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [version :as v]
            [clojure.string :as str]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]
            [sinostudy.views.dictionary :as vd]
            [sinostudy.pages.core :as pages]
            [sinostudy.dictionary.core :as d]))

;;;; HELPER FUNCTIONS

(defn navlink
  [from to text]
  (let [key (str from "->" to)]
    (if (= from to)
      [:a.current-page
       {:key key}
       text]
      [:a
       {:on-click #(rf/dispatch [::events/reset-scroll-state
                                 [::pages/static to]])
        :href     to
        :key      key}
       text])))

(defn navify [from links]
  (map (fn [[to text]] (navlink from to text)) links))

(def year-string
  (let [year (.getFullYear (js/Date.))]
    (if (> year 2018)
      (str "2018-" year)
      "2018")))


;;;; VIEWS

(defn logo
  "The site logo (part of the header trifecta)."
  []
  [:img {:src "/img/logo_min.svg"}])

;; The smart input field.
;; All key presses are also handled from here.
(defn input-field []
  "The input field (part of the header form)."
  (let [input   @(rf/subscribe [::subs/input])
        actions @(rf/subscribe [::subs/actions])
        page    @(rf/subscribe [::subs/current-page])
        hint    @(rf/subscribe [::subs/hint])]
    [:input#input-field
     {:type          :text
      :title         hint
      :placeholder   (events/input-title page)
      :auto-complete "off"
      :disabled      (not (nil? actions))
      :value         input
      :on-change     (fn [e]
                       (when (nil? actions)
                         (rf/dispatch [::events/on-input-change
                                       (-> e .-target .-value)])))}]))

;; The button is not actually displayed!
;; It's kept around to prevent "Enter" submitting the input to an unknown href.
;; If the button isn't there, pressing enter to select an action in the
;; action-chooser can misfire a submit event. The on-click event in the submit
;; button captures these submit events and sends straight them to /dev/null.
(defn input-button []
  "The hidden input button (part of the header form).
  Secretly enables the input field to submit when pressing enter."
  (let [input (rf/subscribe [::subs/input])]
    [:button
     {:type     :submit
      :on-click (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [::events/submit @input]))}
     "go"]))

(defn filters
  "Filter for what type of dictionary search result should be shown."
  []
  (let [{search-term ::d/term} @(rf/subscribe [::subs/content])
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

(defn form []
  "The form (part of the header trifecta)."
  [:form
   {:auto-complete "off"}
   [:div#header-input
    [input-field]
    [input-button]]
   [filters]])

;; Project version number based on git tag + commit SHA
;; More info: https://github.com/roomkey/lein-v
(def github-link
  [:a {:href "https://github.com/simongray/sino.study"} (str "v" v/version)])

(defn header
  "The header contains the logo and the main input form."
  []
  [:header
   [:div#aligner
    [logo]
    [form]
    [:address "© " year-string " Simon Gray (" github-link ")"]]])

(defn article
  "The content pane of the site."
  []
  (reagent/create-class
    {:display-name
     "article"

     :reagent-render
     (fn []
       (let [category     @(rf/subscribe [::subs/current-category])
             content      @(rf/subscribe [::subs/content])
             result-types @(rf/subscribe [::subs/current-result-types])]
         (when content
           [:article
            {:class (when (> (count result-types) 1) "with-filters")}
            (cond
              (= ::pages/terms category) [vd/dictionary-page]
              :else content)])))

     ;; Ensures that scroll state is restored when pushing back/forward button.
     ;; Sadly, this behaviour is global for all updates, so links/buttons/etc.
     ;; must manually dispatch ::events/reset-scroll-state to avoid this!
     :component-did-update
     (fn [_ _]
       (let [page @(rf/subscribe [::subs/current-page])]
         (rf/dispatch [::events/load-scroll-state page])))}))

(defn script-changer []
  "The button used to toggle traditional/simplified Chinese script."
  (let [script     @(rf/subscribe [::subs/script])
        text       (if (= ::d/simplified script)
                     "Simpl."
                     "Trad.")
        alt-script (if (= ::d/simplified script)
                     ::d/traditional
                     ::d/simplified)
        title      (str "Click to use " (if (= ::d/simplified alt-script)
                                          "simplified characters"
                                          "traditional characters"))]
    [:a#script-changer
     {:key      alt-script
      :title    title
      :on-click #(rf/dispatch [::events/change-script alt-script])}
     text]))

(defn footer []
  "The footer (contains navigation and copyright notice)."
  (let [from  @(rf/subscribe [::subs/current-nav])
        links [["/" "Home"] ["/help" "Help"] ["/about" "About"]]]
    [:footer
     [:nav (interpose " · "
             (conj (vec (navify from links))
                   [script-changer {:key "script-changer"}]))]]))

(defn- action-text
  [[action query]]
  (case action
    ::events/look-up (str "Look up " query)
    ::events/digits->diacritics "Convert to diacritics"
    ::events/diacritics->digits "Convert to digits"
    ::events/close-action-chooser "Cancel"))

(defn- action-choice
  [checked action]
  [:li {:key action}
   [:input {:type     :radio
            :name     "action"
            :value    action
            :checked  (= action checked)
            :id       action
            :on-click (fn [e]
                        (.preventDefault e)
                        (rf/dispatch [::events/choose-action action]))}]
   [:label {:for action} (action-text action)]])

(defn action-chooser []
  "The pop-in dialog that is used to select from different possible options."
  (let [actions @(rf/subscribe [::subs/actions])
        checked @(rf/subscribe [::subs/checked-action])]
    (when actions
      [:fieldset#actions
       [:legend "Select an action"]
       [:ol
        (map (partial action-choice (nth actions checked)) actions)]])))

(defn app []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/current-nav]))]
    [:main {:class (when not-home? "with-article")}
     [action-chooser]
     [header]
     [article]
     [footer]]))
