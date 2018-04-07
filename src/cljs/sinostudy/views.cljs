(ns sinostudy.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]
            [sinostudy.pages.defaults :as pd]
            [sinostudy.rim.core :as rim]
            [sinostudy.pinyin.core :as p]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.embed :as embed]
            [sinostudy.dictionary.entry :as entry]))

;; TODO: add bookmark icon for entries and way to get to bookmarks in nav bar
;; TODO: hanzi ref not picking up 的 in 底 entry

;;;; HELPER FUNCTIONS

(defn add-word-links
  [text]
  (let [href    #(str "/" (name pd/terms) "/" %)
        ids     (range (count text))
        link-up (fn [word id] [:a {:title (str "look up " word)
                                   :href  (href word)
                                   :key   id} word])]
    (map link-up text ids)))

;; used both in nav and on dictionary entry pages
(defn script-changer-link
  [script content]
  (let [alt-script (if (= d/simp script) d/trad d/simp)]
    [:a
     {:key      alt-script
      :class    "script-changer fake-link"
      :title    (str "Click to use " (if (= d/simp alt-script)
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
        href           (str "/" (name pd/terms) "/" word "/" id)
        definitions    (d/defs entry)
        false-variant? (contains? (:false-variant entry) script)
        key            (if false-variant? (str "false-" href) href)]
    [:li {:key   key
          :class (if false-variant? "false-variant" "")
          :title (if false-variant? "not a variant in the current script"
                                    "go to dictionary entry")}
     [:a {:href href, :key key}
      (interpose " "
        [(if (= d/simp script)
           [:span.simplified.hanzi {:key "hanzi"} (d/simp entry)]
           [:span.traditional.hanzi {:key "hanzi"} (d/trad entry)])
         [:span.pinyin {:key "pinyin"} (str/join " " (d/pinyin entry))]
         (interpose "; "
           (for [definition definitions]
             (let [only-hanzi  (comp script d/refr->m)
                   definition* (-> definition
                                   (rim/re-handle
                                     embed/refr only-hanzi)
                                   (rim/re-handle
                                     embed/pinyin p/digits->diacritics))]
               [:span.definition {:key definition} definition*])))])]]))

(defn entries->hiccup
  "Convert a list of dictionary entries into hiccup."
  [word entries script]
  (let [ids       (range (count entries))
        entries*  (->> entries
                       (map #(assoc %2 :id %1) ids)
                       (entry/tag-false-variants script))
        to-hiccup (partial entry-li word script)]
    [:div
     [:h1.list-header word]
     [:p.list-subheader (count entries*) " entries found"]
     [:ul.dictionary-entries
      (map to-hiccup entries*)]]))

(defn entry->hiccup
  "Convert a single dictionary entry into hiccup."
  [entry script]
  (let [traditional         (d/trad entry)
        simplified          (d/simp entry)
        definitions         (d/defs entry)
        classifiers         (d/cls entry)
        script-differences? (not (= simplified traditional))
        word                (if (= d/simp script) simplified traditional)]
    [:div.dictionary-entry
     [:h1 (if (= d/simp script)
            [:span.hanzi.simplified (add-word-links simplified)]
            [:spanl.hanzi.traditiona (add-word-links traditional)])]
     [:p.subheader
      (interpose " "
        [[:span.pinyin {:key d/pinyin}
          (interpose " " (add-word-links (d/pinyin entry)))]
         (when script-differences?
           [:span.tag {:key :script}
            (if (= d/simp script) "tr.|" "s.|")
            (script-changer-link
              script
              (if (= d/simp script)
                [:span.hanzi.traditional traditional]
                [:span.hanzi.simplified simplified]))])
         (when classifiers
           [:span.tag {:key d/cls, :title (str "classifiers for " word)}
            "cl.|"
            (interpose ", "
              ;; TODO: sort - currently unsorted!
              (for [classifier classifiers]
                (if (= d/simp script)
                  [:span.hanzi.simplified {:key (d/simp classifier)}
                   (add-word-links (d/simp classifier))]
                  [:span.hanzi.traditional {:key (d/trad classifier)}
                   (add-word-links (d/trad classifier))])))])])]
     [:ol
      (for [definition definitions]
        (let [link        (comp add-word-links vector)
              refr-f      (comp link script d/refr->m)
              index       (fn [script coll]
                            (get coll (cond
                                        (= 1 (count coll)) 0
                                        (= d/simp script) 1
                                        :else 0)))
              script*     (partial index script)
              hanzi-f     (comp link script* #(str/split % #"\|"))
              pinyinize   (fn [s] [:span.pinyin {:key "pinyin"} s])
              no-brackets #(subs % 1 (dec (count %)))
              ;; TODO: remove spaces from href for proper linking
              pinyin-f    (comp pinyinize link p/digits->diacritics no-brackets)
              definition* (-> definition
                              (rim/re-handle embed/refr refr-f)
                              (rim/re-handle embed/hanzi hanzi-f)
                              (rim/re-handle embed/pinyin pinyin-f))]
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
    (= pd/terms page-type) (render-word page-key content script)
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

(defn page []
  (let [script  @(rf/subscribe [::subs/script])
        page    @(rf/subscribe [::subs/current-page])
        pages   @(rf/subscribe [::subs/pages])
        content (when page
                  (get-in pages page))]
    (when content
      [:div.pedestal
       [:article {:key (str page)}
        (render-page page content script)]])))

(defn script-changer []
  (let [script @(rf/subscribe [::subs/script])
        text   (if (= d/simp script) "Simpl." "Trad.")]
    (script-changer-link script text)))

(defn footer []
  (let [from  @(rf/subscribe [::subs/current-nav])
        links [["/" "Home"] ["/help" "Help"] ["/about" "About"]]]
    [:footer
     [:nav (interpose " · "
             (conj (vec (navify from links))
                   [script-changer {:key "script-changer"}]))]
     [:p#copyright "© " year-string " Simon Gray ("
      [:a {:href "https://github.com/simongray"} "github"] ")"]]))

(defn- action-text
  [action]
  (case (first action)
    ::events/look-up (str "Look up word")
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
     [page]
     [footer]]))
