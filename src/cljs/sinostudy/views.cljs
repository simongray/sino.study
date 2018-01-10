(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [sinostudy.site :as site]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]
            [sinostudy.dictionary.common :as dict]))

;;;; HELPER FUNCTIONS

(defn add-word-links
  [text]
  (let [href    #(str "/word/" %)
        ids     (range (count text))
        link-up (fn [word id] [:a {:title (str "look up " word)
                                   :href (href word)
                                   :key id} word])]
    (map link-up text ids)))

;; used both in nav and on dictionary entry pages
(defn script-changer-link
  [script content]
  (let [alt-script (if (= :simplified script) :traditional :simplified)]
    [:a
     {:key      alt-script
      :class    "script-changer fake-link"
      :title    (str "Click to use " (if (= :simplified alt-script)
                                       "simplified characters"
                                       "traditional characters"))
      :on-click #(rf/dispatch [::events/change-script alt-script])}
     content]))

(defn handle-refs
  "Apply function f to all hanzi refs in definition."
  [definition f]
  (let [hanzi-refs  (dict/get-refs definition)]
    (if (empty? hanzi-refs)
      definition
      (interleave (str/split definition dict/hanzi-ref)
                  (map f hanzi-refs)))))

(defn entry-li
  "Converts a dictionary entry into a hiccup list item."
  [word script [entry id]]
  (let [href (str "/word/" word "/" id)
        definitions (:definition entry)]
    [:li {:key href}
     [:a {:href href, :key href}
      (interpose " "
        [(if (= :simplified script)
           [:span.simplified.hanzi {:key "hanzi"} (:simplified entry)]
           [:span.traditional.hanzi {:key "hanzi"} (:traditional entry)])
         [:span.pinyin {:key "pinyin"} (str/join " " (:pinyin entry))]
         (interpose "; "
           (for [definition definitions]
             (let [definition* (handle-refs definition script)]
               [:span.definition {:key definition} definition*])))])]]))

(defn entries->hiccup
  "Convert a list of dictionary entries into hiccup."
  [word entries script]
  [:div
   [:h1.list-header word]
   [:ul.dictionary-entries
    (let [ids         (range (count entries))
          entry-li*   (partial entry-li word script)
          entries+ids (map list entries ids)]
      (map entry-li* entries+ids))]])

(defn entry->hiccup
  "Convert a single dictionary entry into hiccup."
  [entry script]
  (let [traditional         (:traditional entry)
        simplified          (:simplified entry)
        definitions         (:definition entry)
        classifiers         (:classifiers entry)
        script-differences? (not (= simplified traditional))
        word                (if (= :simplified script) simplified traditional)]
    [:div.dictionary-entry
     [:h1 (if (= :simplified script)
            [:span.hanzi.simplified (add-word-links simplified)]
            [:spanl.hanzi.traditiona (add-word-links traditional)])]
     [:p.subheader
      (interpose " "
        [[:span.pinyin {:key :pinyin}
          (interpose " " (add-word-links (:pinyin entry)))]
         (when script-differences?
           [:span.tag {:key :script}
            (if (= :simplified script) "tr.|" "s.|")
            (script-changer-link
              script
              (if (= :simplified script)
                [:span.hanzi.traditional traditional]
                [:span.hanzi.simplified simplified]))])
         (when classifiers
           [:span.tag {:key :classifiers, :title (str "classifiers for " word)}
            "cl.|"
            (interpose ", "
              (for [classifier (dict/sort-classifiers classifiers)]
                (if (= :simplified script)
                  [:span.hanzi.simplified {:key (:simplified classifier)}
                   (add-word-links (:simplified classifier))]
                  [:span.hanzi.traditional {:key (:traditional classifier)}
                   (add-word-links (:traditional classifier))])))])])]
     [:ol
      (for [definition definitions]
        (let [link        (comp add-word-links vector script)
              definition* (handle-refs definition link)]
          [:li {:key definition} [:span.definition definition*]]))]]))

(defn unknown-word
  [word]
  [:h1 "Unknown word"
   [:p "There are no dictionary entries for the word \"" word "\"."]])

(defn render-word
  "Render a word page for display; word can be a vector or a map (one entry)."
  [word content script]
  (if (sequential? content)
    (case (count content)
      0 (unknown-word word)
      1 (entry->hiccup (first content) script)
      (entries->hiccup word content script))
    (entry->hiccup content script)))

(defn render-page
  "Render a page for display based on the page-type and content."
  [[page-type page-key] content script]
  (case page-type
    :static (:content content)
    :word (render-word page-key content script)))

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
  (let [nav @(rf/subscribe [::subs/nav])]
    [:header
     [:a {:href "/"}
      [:img#logo {:src "/img/logo_min.svg"
                  :class (if (= "/" nav)
                           "big-logo"
                           "small-logo")}]]]))

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

;; not actually displayed!
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
        page    @(rf/subscribe [::subs/page])
        script  @(rf/subscribe [::subs/script])]
    (when content
      [:div.pedestal
       [:article {:key (str page)}
        (render-page page content script)]])))

(defn script-changer []
  (let [script @(rf/subscribe [::subs/script])
        text   (if (= :simplified script) "Simpl." "Trad.")]
    (script-changer-link script text)))

(defn footer []
  (let [from  @(rf/subscribe [::subs/nav])
        links [["/" "Home"] ["/help" "Help"] ["/about" "About"]]]
    [:footer
     [:nav (interpose " · "
             (conj (vec (navify from links))
                   [script-changer {:key "script-changer"}]))]
     [:p#copyright "© " year-string " Simon Gray ("
      [:a {:href "https://github.com/simongray"} "github"] ")"]]))

(defn main-panel []
  (let [not-home? (not= "/" @(rf/subscribe [::subs/nav]))]
    [:div#bg {:class (if not-home? "with-page" "")}
     [:div {:class (if not-home? "main top" "main")}
      [:div#aligner
       [header]]]
     [page]
     [footer]]))
