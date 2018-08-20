(ns sinostudy.views.dictionary
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.core :as p]
            [sinostudy.views.common :as vc]
            [sinostudy.subs :as subs]
            [sinostudy.pages.core :as pages]))

;; TODO: refactor this whole namespace, too many args to functions

(defn entry-title
  "The title of the term with links to characters -OR- decomposition
  into components if the term is a character."
  []
  (let [script         @(rf/subscribe [::subs/script])
        {term          ::d/term
         decomposition ::d/decomposition} @(rf/subscribe [::subs/content])
        attribute      @(rf/subscribe [::subs/current-attribute])
        zh             (vc/zh script)
        decomposition* (when (not= decomposition "？") decomposition)]
    (cond
      (> (count term) 1)
      [:h1 {:lang zh} (vc/link-term term)]

      (= attribute "decomposition")
      [:h1 {:lang  zh
            :title (str "Character decomposition")}
       (map vc/hanzi-link decomposition*)]

      decomposition*
      [:h1
       {:lang  zh
        :title (str "Click to decompose")}
       [:a
        {:href (str "/" (name ::pages/terms) "/" term "/decomposition")}
        term]]

      :else
      [:h1
       {:lang  zh
        :title term}
       term])))

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
    [:<>
     (for [[pinyin definitions] uses]
       [:<> {:key pinyin}
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

(defn info-table
  "Additional information about the dictionary entry."
  []
  (let [script       @(rf/subscribe [::subs/script])
        zh           (vc/zh script)
        {term        ::d/term
         radical     ::d/radical
         frequency   ::d/frequency
         variations  ::d/variations
         classifiers ::d/classifiers
         etymology   ::d/etymology} @(rf/subscribe [::subs/content])
        label        (d/frequency-label frequency)
        entry-script (cond
                       (contains? variations ::d/traditional) ::d/traditional
                       (contains? variations ::d/simplified) ::d/simplified)
        entry-zh     (vc/zh entry-script)]
    [:table.info
     [:tbody
      [:tr {:key   ::d/frequency
            :title "Word frequency"}
       [:td "frequency"]
       [:td (cond
              (= label :high) [:span.frequency-high "frequent"]
              (= label :medium) [:span.frequency-medium "average"]
              (= label :low) [:span.frequency-low "infrequent"])]]
      (when entry-script
        [:tr {:key   ::d/variations
              :title (str (if (= ::d/traditional entry-script)
                            "In Traditional Chinese"
                            "In Simplified Chinese"))}
         (if (= entry-script ::d/traditional)
           [:td "traditional"]
           [:td "simplified"])
         [:td {:lang entry-zh}
          (interpose ", " (->> variations
                               entry-script
                               (map vector)
                               (map vc/link-term)
                               (map (fn [variation]
                                      [:span {:key variation}
                                       variation]))))]])
      (when classifiers
        [:tr {:key   ::d/classifiers
              :title (str "Common classifiers")}
         [:td "classifier"]
         [:td
          (interpose ", "
            (for [classifier (sort-by ::d/pinyin classifiers)]
              [:span
               {:lang zh
                :key  (script classifier)}
               (vc/link-term (vector (script classifier)))]))]])
      (when radical
        [:tr {:key   ::d/radical
              :title "Radical"}
         [:td "radical"]
         (if (= term radical)
           [:td "The character is a radical"]
           [:td {:lang zh} (vc/link-term (vector radical))])])
      (when etymology
        (let [{type     ::d/type
               hint     ::d/hint
               semantic ::d/semantic
               phonetic ::d/phonetic} etymology]
          [:tr {:key   ::d/etymology
                :title "Etymology"}
           [:td "etymology"]
           [:td {:title (str "Type: " type)}
            (cond
              (and (or (= type "pictographic") (= type "ideographic")) hint)
              [:<> (let [link (comp vc/link-term vector)]
                     (vc/handle-refs script link hint))]

              (and (= type "pictophonetic") semantic phonetic)
              [:<>
               [:span {:lang zh} (vc/link-term semantic)]
               " (" hint ") + "
               [:span {:lang zh} (vc/link-term phonetic)]])]]))]]))

(defn entry
  "Dictionary entry for a specific term."
  []
  [:div.dictionary-entry
   [entry-title]
   [:div.usages
    [usage-list]
    [info-table]]])

(defn- result-entry-uses
  "Listed uses of a search result entry."
  [script term uses]
  (for [[pronunciation definitions] uses]
    (when (not (empty? definitions))
      (let [handle-refs* (partial vc/handle-refs script identity)
            definitions* (->> definitions
                              (no-fake-variants script term)
                              (map handle-refs*))]
        [:li {:key pronunciation}
         [:span.pinyin
          (p/digits->diacritics pronunciation)]
         " "
         [:span.definition
          (interpose "; " definitions*)]]))))

(defn- search-result-entry
  "Entry in a results-list."
  [script
   {term ::d/term
    uses ::d/uses}]
  (when-let [entry-uses (result-entry-uses script term uses)]
    [:li {:key term}
     [:a
      {:href (str "/" (name ::d/terms) "/" term)}
      [:span {:lang (vc/zh script)} term]
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
      (into [:ul.dictionary-entries]
            (->> entries
                 (filter in-script)
                 (map (partial search-result-entry script)))))))

(defn search-result
  "Dictionary search result."
  []
  [:div.search-result
   [search-result-entries]])

(defn unknown-term
  "Dictionary entry for a term that does not exist."
  []
  [:p "There are no dictionary entries available for this term."])

(defn dictionary-page
  "A dictionary page can be 1 of 3 types: entry, search result, or unknown."
  []
  (let [content @(rf/subscribe [::subs/content])]
    (cond
      (::d/uses content) [entry]
      (> (count (keys content)) 1) [search-result]
      :else [unknown-term])))
