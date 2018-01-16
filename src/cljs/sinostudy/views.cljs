(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]
            [sinostudy.pages.defaults :as pd]
            [sinostudy.dictionary.defaults :as dd]
            [sinostudy.rim.core :as rim]
            [sinostudy.pinyin.patterns :as pp]
            [sinostudy.dictionary.core :as dict]))

;;;; HELPER FUNCTIONS

(defn add-word-links
  [text]
  (let [href    #(str "/" (name pd/words) "/" %)
        ids     (range (count text))
        link-up (fn [word id] [:a {:title (str "look up " word)
                                   :href  (href word)
                                   :key   id} word])]
    (map link-up text ids)))

;; used both in nav and on dictionary entry pages
(defn script-changer-link
  [script content]
  (let [alt-script (if (= dd/simp script) dd/trad dd/simp)]
    [:a
     {:key      alt-script
      :class    "script-changer fake-link"
      :title    (str "Click to use " (if (= dd/simp alt-script)
                                       "simplified characters"
                                       "traditional characters"))
      :on-click #(rf/dispatch [::events/change-script alt-script])}
     content]))

;; TODO: for single char entries, make top entry link split into components
;;       replace the char with a table of the components based on the ordering:
;;       https://en.wikipedia.org/wiki/Chinese_character_description_language#Ideographic_Description_Sequences
;;       CHISE can be used (slow), but perhaps skishore/makemeahanzi is better
(defn entry-li
  "Converts a dictionary entry into a hiccup list item."
  [word script entry]
  (let [id             (:id entry)
        href           (str "/" (name pd/words) "/" word "/" id)
        definitions    (dd/defs entry)
        false-variant? (contains? (:false-variant entry) script)
        key            (if false-variant? (str "false-" href) href)]
    [:li {:key   key
          :class (if false-variant? "false-variant" "")
          :title (if false-variant? "not a variant in the current script"
                                    "go to dictionary entry")}
     [:a {:href href, :key key}
      (interpose " "
        [(if (= dd/simp script)
           [:span.simplified.hanzi {:key "hanzi"} (dd/simp entry)]
           [:span.traditional.hanzi {:key "hanzi"} (dd/trad entry)])
         [:span.pinyin {:key "pinyin"} (str/join " " (dd/pinyin entry))]
         (interpose "; "
           (for [definition definitions]
             (let [f           (comp script dict/hanzi-ref->m)
                   definition* (rim/re-handle definition dict/hanzi-ref f)]
               [:span.definition {:key definition} definition*])))])]]))

(defn entries->hiccup
  "Convert a list of dictionary entries into hiccup."
  [word entries script]
  (let [ids       (range (count entries))
        entries*  (->> entries
                       (map #(assoc %2 :id %1) ids)
                       (dict/tag-false-variants script))
        to-hiccup (partial entry-li word script)]
    [:div
     [:h1.list-header word]
     [:p.list-subheader (count entries*) " entries found"]
     [:ul.dictionary-entries
      (map to-hiccup entries*)]]))

(defn entry->hiccup
  "Convert a single dictionary entry into hiccup."
  [entry script]
  (let [traditional         (dd/trad entry)
        simplified          (dd/simp entry)
        definitions         (dd/defs entry)
        classifiers         (dd/cls entry)
        script-differences? (not (= simplified traditional))
        word                (if (= dd/simp script) simplified traditional)]
    [:div.dictionary-entry
     [:h1 (if (= dd/simp script)
            [:span.hanzi.simplified (add-word-links simplified)]
            [:spanl.hanzi.traditiona (add-word-links traditional)])]
     [:p.subheader
      (interpose " "
        [[:span.pinyin {:key dd/pinyin}
          (interpose " " (add-word-links (dd/pinyin entry)))]
         (when script-differences?
           [:span.tag {:key :script}
            (if (= dd/simp script) "tr.|" "s.|")
            (script-changer-link
              script
              (if (= dd/simp script)
                [:span.hanzi.traditional traditional]
                [:span.hanzi.simplified simplified]))])
         (when classifiers
           [:span.tag {:key dd/cls, :title (str "classifiers for " word)}
            "cl.|"
            (interpose ", "
              ;; TODO: sort - currently unsorted!
              (for [classifier classifiers]
                (if (= dd/simp script)
                  [:span.hanzi.simplified {:key (dd/simp classifier)}
                   (add-word-links (dd/simp classifier))]
                  [:span.hanzi.traditional {:key (dd/trad classifier)}
                   (add-word-links (dd/trad classifier))])))])])]
     [:ol
      (for [definition definitions]
        (let [link        (comp add-word-links vector)
              hanzi-ref-f (comp link script dict/hanzi-ref->m)
              index       (fn [script coll] (get coll (cond
                                                        (= 1 (count coll)) 0
                                                        (= dd/simp script) 1
                                                        :else 0)))
              hanzi-f     (comp link (partial index script) dict/split-hanzi)
              definition* (-> definition
                              (rim/re-handle dict/hanzi-ref hanzi-ref-f)
                              (rim/re-handle dict/hanzi hanzi-f))]
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
  (cond
    (= pd/words page-type) (render-word page-key content script)
    :else content))

(defn navlink
  [from to text]
  (let [key (str from "->" to)]
    (if (= from to)
      [:a.current-page {:key key} text]
      [:a {:href to :key key} text])))

(defn navify [from links]
  (map (fn [[to text]] (navlink from to text)) links))

(def year-string
  (let [year (.getFullYear (js/Date.))]
    (if (> year 2017)
      (str "2017-" year)
      "2017")))


;;;; VIEWS

(defn logo []
  (let [nav @(rf/subscribe [::subs/nav])]
    [:header
     [:a {:href "/"}
      [:img#logo {:src   "/img/logo_min.svg"
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
        text   (if (= dd/simp script) "Simpl." "Trad.")]
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
