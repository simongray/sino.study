(ns sinostudy.views.dictionary
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.core :as p]
            [sinostudy.views.common :as vc]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]
            [sinostudy.pages.core :as pages]))

;; TODO: refactor this whole namespace, too many args to functions

(defn term-title
  "The title of the term with links to characters -OR- decomposition
  into components if the term is a character."
  []
  (let [{term          ::d/term
         decomposition ::d/decomposition} @(rf/subscribe [::subs/content])
        attribute      @(rf/subscribe [::subs/current-attribute])
        decomposition* (when (not= decomposition "？") decomposition)]
    (if (> (count term) 1)
      [:span.hanzi (vc/link-term term)]
      (cond
        (= attribute "decomposition")
        [:span.hanzi {:title (str "Character decomposition")}
         (map vc/hanzi-link decomposition*)]

        decomposition*
        [:span.hanzi
         {:key   ::d/term
          :title (str "Click to decompose")}
         [:a
          {:href (str "/" (name ::pages/terms) "/" term "/decomposition")}
          term]]

        :else
        [:span.hanzi
         {:title term}
         term]))))

(defn etymology-blurb
  "Etymology information about a specific character."
  []
  (let [script @(rf/subscribe [::subs/script])
        {etymology ::d/etymology} @(rf/subscribe [::subs/content])]
    (when etymology
      (let [{type     ::d/type
             hint     ::d/hint
             semantic ::d/semantic
             phonetic ::d/phonetic} etymology]
        [:div.dictionary-header-bubble
         (cond
           (and (or (= type "pictographic")
                    (= type "ideographic"))
                hint)
           [:div.etymology
            {:title (str "Type: " type)}
            (let [link (comp vc/link-term vector)]
              (vc/handle-refs script link hint))]

           (and (= type "pictophonetic")
                semantic
                phonetic)
           [:div.etymology
            {:title (str "Type: " type)}
            [:span.hanzi (vc/link-term semantic)]
            " (" hint ") + "
            [:span.hanzi (vc/link-term phonetic)]])]))))

(defn dictionary-header
  "Dictionary entry header."
  []
  [:div.dictionary-header
   [:h1 [term-title]]
   [etymology-blurb]])

(defn frequency-tag
  "A tag with a frequency label based on a word frequency."
  []
  (let [{frequency ::d/frequency} @(rf/subscribe [::subs/content])
        label (d/frequency-label frequency)]
    [:span.tag
     {:key   ::d/frequency
      :title "Word frequency"}
     (cond
       (= label :high) [:span.frequency-high "frequent"]
       (= label :medium) [:span.frequency-medium "average"]
       (= label :low) [:span.frequency-low "infrequent"])]))

(defn variations-tag
  "Tag with the variations of an entry in a given script."
  []
  (let [{variations ::d/variations} @(rf/subscribe [::subs/content])
        script (cond
                 (contains? variations ::d/traditional) ::d/traditional
                 (contains? variations ::d/simplified) ::d/simplified
                 :else nil)]
    (when script
      [:span.tag
       {:key   ::d/variations
        :title (str (if (= ::d/traditional script)
                      "In Traditional Chinese"
                      "In Simplified Chinese"))}
       (if (= script ::d/traditional)
         "tr.|"
         "s.|")
       (interpose ", " (->> variations
                            script
                            (map vector)
                            (map vc/link-term)
                            (map (fn [variation]
                                   [:span.hanzi
                                    {:key variation}
                                    variation]))))])))

(defn classifiers-tag
  "Tag with the classifiers of an entry in a given script."
  []
  (let [script @(rf/subscribe [::subs/script])
        {classifiers ::d/classifiers} @(rf/subscribe [::subs/content])]
    (when classifiers
      [:span.tag {:key   ::d/classifiers
                  :title (str "Common classifiers")}
       "cl.|"
       (interpose ", "
         (for [classifier (sort-by ::d/pinyin classifiers)]
           [:span.hanzi
            {:key (script classifier)}
            (vc/link-term (vector (script classifier)))]))])))

(defn radical-tag
  "Tag with the radical of a Hanzi."
  []
  (let [{term    ::d/term
         radical ::d/radical} @(rf/subscribe [::subs/content])]
    (when radical
      (if (= term radical)
        [:span.tag
         {:key   ::d/radical
          :title "The character is a radical"}
         "radical"]
        [:span.tag
         {:key   ::d/radical
          :title (str "Radical")}
         "rad.|"
         [:span.hanzi (vc/link-term (vector radical))]]))))

(defn tags
  "Available tags of a dictionary entry."
  []
  [:p.subheader
   (interpose " " [[frequency-tag]
                   [variations-tag]
                   [classifiers-tag]
                   [radical-tag]])])

;; In certain cases, entries may include these "fake" definitions.
;; They're removed on the frontend since the variant may well be valid in
;; .e.g. traditional Chinese, but not in simplified Chinese (see: 喂).
(defn no-fake-variants
  "Removes definitions of the pattern 'variant of _' if the term is identical."
  [script term definitions]
  (if (= (count term) 1)
    (let [variant-re (re-pattern (if (= script ::d/traditional)
                                   (str "variant of " term)
                                   (str "variant of " term
                                        "\\[|variant of .\\|" term)))]
      (filter (comp not (partial re-find variant-re)) definitions))
    definitions))

(defn usage-list
  "List of definitions for each Pinyin variation of an entry."
  []
  (let [script @(rf/subscribe [::subs/script])
        {term ::d/term
         uses ::d/uses} @(rf/subscribe [::subs/content])]
    [:div
     (for [[pinyin definitions] uses]
       [:div {:key pinyin}
        [:h2.pinyin
         (->> (str/split pinyin #" ")
              (map p/digits->diacritics)
              (map vector)
              (map vc/link-term)
              (interpose " "))]
        [:ol
         (for [definition (no-fake-variants script term definitions)]
           [:li {:key definition}
            [:span.definition
             (let [link (comp vc/link-term vector)]
               (vc/handle-refs script link definition))]])]])]))

(defn entry
  "Dictionary entry for a specific term."
  []
  [:div.dictionary-entry
   [dictionary-header]
   [tags]
   [usage-list]])

(defn search-result-header
  "The header for a dictionary search result."
  []
  (let [{search-term ::d/term} @(rf/subscribe [::subs/content])]
    [:h1.list-header "\"" search-term "\""]))

(defn search-result-filter
  "Filter for what type of dictionary search result should be shown."
  []
  (let [{search-term ::d/term} @(rf/subscribe [::subs/content])
        current-filter @(rf/subscribe [::subs/current-result-filter])
        result-types   @(rf/subscribe [::subs/current-result-types])]
    (when (> (count result-types) 1)
      [:form
       (->> (for [result-type result-types]
              [:label {:key result-type}
               [:input {:type      "radio"
                        :name      "result-filter"
                        :value     result-type
                        :checked   (= current-filter result-type)
                        :on-change (fn [_]
                                     (rf/dispatch [::events/set-result-filter
                                                   search-term
                                                   result-type]))}]
               (str/capitalize (name result-type))])
            (interpose " "))])))

(defn- result-entry-uses
  "Listed uses of a search result entry."
  [script term uses]
  (for [[pronunciation definitions] uses]
    (when (not (empty? definitions))
      (let [handle-refs* (partial vc/handle-refs script identity)
            definitions* (no-fake-variants script term definitions)]
        [:li
         {:key pronunciation}
         [:span.pinyin
          {:key pronunciation}
          (p/digits->diacritics pronunciation)]
         " "
         [:span.definition
          (interpose "; " (map handle-refs* definitions*))]]))))

(defn- search-result-entry
  "Entry in a results-list."
  [script
   {term ::d/term
    uses ::d/uses}]
  (when-let [entry-uses (result-entry-uses script term uses)]
    [:li {:key term}
     [:a
      {:href (str "/" (name ::d/terms) "/" term)}
      [:span.hanzi term]
      " "
      [:ul
       entry-uses]]]))

(defn- in-script
  "For filtering entries by script."
  [entry]
  (let [script @(rf/subscribe [::subs/script])]
    (contains? (::d/scripts entry) script)))

(defn search-result-entries
  "List of search result entries."
  []
  (let [script        @(rf/subscribe [::subs/script])
        content       @(rf/subscribe [::subs/content])
        result-filter @(rf/subscribe [::subs/current-result-filter])]
    (when-let [entries (get content result-filter)]
      [:ul.dictionary-entries
       (doall (for [entry (filter in-script entries)]
                (search-result-entry script entry)))])))

(defn search-result
  "Dictionary search result."
  []
  [:div.search-result
   [search-result-header]
   [search-result-filter]
   [search-result-entries]])

(defn unknown-term
  "Dictionary entry for a term that does not exist."
  []
  (let [{search-term ::d/term} @(rf/subscribe [::subs/content])]
    [:div.search-result
     [:h1.list-header "\"" search-term "\""]
     [:p "There are no dictionary entries available for this term."]]))

(defn dictionary-page
  "A dictionary page can be 1 of 3 types: entry, search result, or unknown."
  []
  (let [content @(rf/subscribe [::subs/content])]
    (cond
      (::d/uses content) [entry]
      (> (count (keys content)) 1) [search-result]
      :else [unknown-term])))
