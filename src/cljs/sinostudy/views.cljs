(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [sinostudy.site :as site]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]))

;;;; HELPER FUNCTIONS

(defn dictionary-li
  "Converts a dictionary entry into a hiccup list item."
  [search-term [entry id]]
  [:li {:key (str entry)}
   [:a {:href (str "/word/" search-term "/" id)}
    (interpose " "
      [[:span.simplified.hanzi (:simplified entry)]
       [:span.traditional.hanzi (:traditional entry)]
       [:span.pinyin (:pinyin entry)]
       (for [definition (:definition entry)]
         [:span.definition definition])])]])

(defn entries->hiccup
  "Convert a list of dictionary entries into hiccup."
  [search-term entries]
  [:div
   [:h1.list-header search-term]
   [:ul.dictionary-entries
    (let [ids            (range (count entries))
          dictionary-li* (partial dictionary-li search-term)
          entries+ids    (map list entries ids)]
      (map dictionary-li* entries+ids))]])

(defn render-page
  "Render a page for display based on the page-type and content."
  [[page-type page-key] content]
  (case page-type
    :static (:content content)
    :word (entries->hiccup page-key content)))

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
               ["/word/蒙" "蒙"]]]                          ;TODO: remove after debugging
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
