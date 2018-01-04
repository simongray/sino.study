(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [sinostudy.site :as site]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]))

;;;; HELPER FUNCTIONS

(defn entry-li
  "Converts a dictionary entry into a hiccup list item."
  [word [entry id]]
  [:li {:key (str entry)}
   [:a {:href (str "/word/" word "/" id)}
    (interpose " "
      [[:span.simplified.hanzi (:simplified entry)]
       [:span.traditional.hanzi (:traditional entry)]
       [:span.pinyin (:pinyin entry)]
       (for [definition (:definition entry)]
         [:span.definition definition])])]])

(defn entries->hiccup
  "Convert a list of dictionary entries into hiccup."
  [word entries]
  [:div
   [:h1.list-header word]
   [:ul.dictionary-entries
    (let [ids         (range (count entries))
          entry-li*   (partial entry-li word)
          entries+ids (map list entries ids)]
      (map entry-li* entries+ids))]])

(defn entry->hiccup
  "Convert a single dictionary entry into hiccup."
  [entry]
  [:div
   [:h1
    [:span.simplified.hanzi (:simplified entry)]
    [:span.traditional.hanzi (:traditional entry)]]
   [:p
    [:span.pinyin (:pinyin entry)]
    [:ol
     (for [definition (:definition entry)]
       [:li {:key definition} [:span.definition definition]])]]])

(defn unknown-word
  [word]
  [:h1 "Unknown word"
   [:p "There are no dictionary entries for the word \"" word "\"."]])

(defn render-word
  "Render a word page for display."
  [word entries]
  (case (count entries)
    0 (unknown-word word)
    1 (entry->hiccup (first entries))
    (entries->hiccup word entries)))

(defn render-page
  "Render a page for display based on the page-type and content."
  [[page-type page-key] content]
  (case page-type
    :static (:content content)
    :word (render-word page-key content)))

(defn navlink
  [from to text]
  (let [key (str from "->" to)]
    (if (= from to)
      [:a.current-page {:key key} text]
      [:a {:href to :key key} text])))

(defn navify [from links]
  (map (fn [[to text]] (navlink from to text)) links))

(def year-string
  (let [year (site/current-year)]
    (if (> year 2017)
      (str "2017-" year)
      "2017")))


;;;; VIEWS

(defn logo []
  [:header
   [:a {:href "/"}
    [:img#logo {:src "/img/logo_min.svg"}]]])

(defn input-field []
  (let [input     (rf/subscribe [::subs/input])
        css-class (rf/subscribe [::subs/input-css-class])]
    [:input#study-input
     {:type      :text
      :value     @input
      :class     @css-class
      :on-change (fn [e]
                   (rf/dispatch [::events/on-input-change
                                 (-> e .-target .-value)]))}]))

(defn input-button []
  (let [input (rf/subscribe [::subs/input])]
    [:button#study-button
     {:type     :submit
      :on-click (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [::events/on-submit @input]))}
     "go"]))

(defn form []
  [:form#study-form
   [input-field]
   [input-button]])

(defn hint []
  (let [hint-key     (rf/subscribe [::subs/hint-type])
        hint-content (rf/subscribe [::subs/hint-content])]
    [:div#study-hint
     {:key @hint-key}
     @hint-content]))

(defn header []
  [:div
   [logo]
   [form]
   [hint]])

(defn page []
  (let [content @(rf/subscribe [::subs/page-content])
        page    @(rf/subscribe [::subs/page])]
    (when content
      [:div.pedestal
       [:article {:key (str page)}
        (render-page page content)]])))

(defn footer []
  (let [from  (rf/subscribe [::subs/nav])
        links [["/" "Home"]
               ["/help" "Help"]
               ["/blog" "Blog"]
               ["/about" "About"]
               ["/word/统治" "统治"]]]                          ;TODO: remove after debugging
    [:footer
     [:nav (interpose " · " (navify @from links))]
     [:p#copyright "© " year-string " Simon Gray ("
      [:a {:href "https://github.com/simongray"} "github"]
      ")"]]))

(defn main-panel []
  (let [content? @(rf/subscribe [::subs/page-content])]
    [:div#bg {:class (if content? "with-page" "")}
     [:div {:class (if content? "main top" "main")}
      [:div#aligner
       [header]]]
     [page]
     [footer]]))
