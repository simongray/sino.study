(ns sinostudy.views.core
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
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
        :href     to
        :key      key}
       text])))

(defn navify [from links]
  (map (fn [[to text]] (navlink from to text)) links))

(def year-string
  (let [year (.getFullYear (js/Date.))]
    (if (> year 2017)
      (str "2017-" year)
      "2017")))


;;;; VIEWS

(defn logo
  "The site logo (part of the header trifecta)."
  []
  [:a {:href "/"}
   [:img {:src "/img/logo_min.svg"}]])

;; The smart input field.
;; All key presses are also handled from here.
(defn input-field []
  "The input field (part of the header form)."
  (let [input   @(rf/subscribe [::subs/input])
        actions @(rf/subscribe [::subs/actions])]
    [:input
     {:type          :text
      :auto-complete "off"
      ;; The #study-input id is required to regain focus (see :set-focus).
      :id            "study-input"
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

(defn form []
  "The form (part of the header trifecta)."
  [:form
   {:auto-complete "off"}
   [input-field]
   [input-button]])

(defn press-enter-to
  [s]
  ["press " [:span.keypress "enter"] " to " s])

;; action-related hints (press-enter-to ...) must match action name!
(def hint-content
  {::events/query-failure       "something went wrong..."
   ::events/no-actions          "not sure what to do with that..."
   ::events/digits->diacritics  (press-enter-to "convert to tone diacritics")
   ::events/diacritics->digits  (press-enter-to "convert to tone digits")
   ::events/look-up             (press-enter-to "look up the term")
   ::events/open-action-chooser (press-enter-to "choose an action")})

(defn hint []
  "The hint (part of the header trifecta).
  Provides helpful hints based on the current input."
  (let [hints   @(rf/subscribe [::subs/hints])
        content (get hint-content (:type (first hints)))]
    (into [:p {:key (count hints)}]
          content)))

(defn header
  "The header trifecta: logo, form and hint."
  []
  [:header
   [:div#aligner
    [logo]
    [form]
    [hint]]])

(defn main
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
           [:main
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

;; Project version number based on git tag + commit SHA
;; More info: https://github.com/roomkey/lein-v
(def github-link
  [:a {:href "https://github.com/simongray/sino.study"} (str "v" v/version)])

(defn footer []
  "The footer (contains navigation and copyright notice)."
  (let [from  @(rf/subscribe [::subs/current-nav])
        links [["/" "Home"] ["/help" "Help"] ["/about" "About"]]]
    [:footer
     [:nav (interpose " · "
             (conj (vec (navify from links))
                   [script-changer {:key "script-changer"}]))]
     [:address "© " year-string " Simon Gray (" github-link ")"]]))

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
      [:form#action-chooser
       [:h1 "Select an action"]
       [:ol
        (map (partial action-choice (nth actions checked)) actions)]])))

(defn app []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/current-nav]))]
    [:div#bg {:class (when not-home? "with-page")}
     [header]
     [action-chooser]
     [main]
     [footer]]))
