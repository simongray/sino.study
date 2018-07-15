(ns sinostudy.views.core
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [version :as v]
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
        :href to
        :key key}
       text])))

(defn navify [from links]
  (map (fn [[to text]] (navlink from to text)) links))

(def year-string
  (let [year (.getFullYear (js/Date.))]
    (if (> year 2017)
      (str "2017-" year)
      "2017")))


;;;; VIEWS

(defn logo []
  (let [nav @(rf/subscribe [::subs/current-nav])]
    [:header
     [:a {:href "/"}
      [:img#logo {:src   "/img/logo_min.svg"
                  :class (if (= "/" nav)
                           "big-logo"
                           "small-logo")}]]]))

;; The smart input field.
;; All key presses are also handled from here.
(defn input-field []
  (let [input      @(rf/subscribe [::subs/input])
        actions    @(rf/subscribe [::subs/actions])
        evaluation @(rf/subscribe [::subs/current-evaluation])
        css-class  (cond
                     actions "default disabled"
                     (and evaluation
                          (empty? (:actions evaluation))
                          (not= "" (:query evaluation))) "default no-actions"
                     :else "default")]
    [:input#study-input
     {:type          :text
      :auto-complete "off"
      :disabled      (not (nil? actions))
      :value         input
      :class         css-class
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
  (let [input (rf/subscribe [::subs/input])]
    [:button#study-button
     {:type     :submit
      :on-click (fn [e] (.preventDefault e)
                  (rf/dispatch [::events/submit @input]))}]))

(defn form []
  [:form#study-form
   {:auto-complete "off"}
   [input-field]
   [input-button]])

(defn hint []
  (let [hints   @(rf/subscribe [::subs/hints])
        hint    (first hints)
        key     (count hints)
        content (get events/hint-contents (:type hint))]
    [:div#study-hint
     {:key key}
     content]))

(defn header []
  [:div
   [logo]
   [form]
   [hint]])

(defn content-pane
  "The main content pane of the site."
  []
  (reagent/create-class
    {:display-name
     "content-pane"

     :reagent-render
     (fn []
       (let [category @(rf/subscribe [::subs/current-category])
             content  @(rf/subscribe [::subs/content])]
         (when content
           [:div.pedestal
            [:article
             (cond
               (= ::pages/terms category) [vd/dictionary-page]
               :else content)]])))

     ;; Ensures that scroll state is restored when pushing back/forward button.
     ;; Sadly, this behaviour is global for all updates, so links/buttons/etc.
     ;; must manually dispatch ::events/reset-scroll-state to avoid this!
     :component-did-update
     (fn [_ _]
       (let [scroll-state @(rf/subscribe [::subs/scroll-state])]
         (when scroll-state
           (rf/dispatch [::events/use-scroll-state scroll-state]))))}))

(defn script-changer []
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
    [:a
     {:key      alt-script
      :class    "script-changer fake-link"
      :title    title
      :on-click #(rf/dispatch [::events/change-script alt-script])}
     text]))

;; Project version number based on git tag + commit SHA
;; More info: https://github.com/roomkey/lein-v
(def github-link
  [:a {:href "https://github.com/simongray/sino.study"} (str "v" v/version)])

(defn footer []
  (let [from  @(rf/subscribe [::subs/current-nav])
        links [["/" "Home"] ["/help" "Help"] ["/about" "About"]]]
    [:footer
     [:nav (interpose " · "
             (conj (vec (navify from links))
                   [script-changer {:key "script-changer"}]))]
     [:p#copyright "© " year-string " Simon Gray (" github-link ")"]]))

(defn- action-text
  [[action query]]
  (case action
    ::events/look-up (str "Look up " query)
    ::events/digits->diacritics "Convert to diacritics"
    ::events/diacritics->digits "Convert to digits"
    ::events/close-action-chooser "Cancel"))

(defn- action-choice
  [marked action]
  [:li {:key   action
        :class (when (= marked action) "marked")}
   [:input {:type     :radio
            :name     "action"
            :value    action
            :id       action
            :on-click (fn [e]
                        (.preventDefault e)
                        (rf/dispatch [::events/choose-action action]))}]
   [:label {:for action} (action-text action)]])

(defn action-chooser []
  (let [actions @(rf/subscribe [::subs/actions])
        marked  @(rf/subscribe [::subs/marked-action])]
    [:form#action-chooser
     {:action ""
      :class  (when (nil? actions) "hidden")}
     [:p#action-header "Select an action"]
     [:ol
      (map (partial action-choice (nth actions marked)) actions)]]))

(defn main-panel []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/current-nav]))]
    [:div#bg {:class (if not-home? "with-page" "")}
     [:div {:class (if not-home? "main top" "main")}
      [:div#aligner
       [header]]]
     [action-chooser]
     [content-pane]
     [footer]]))
