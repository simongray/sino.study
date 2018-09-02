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
    [:section#usages
     [:dl
      (for [[pinyin definitions] uses]
        (let [pinyin* (->> (str/split pinyin #" ")
                           (map p/digits->diacritics)
                           (map vector)
                           (map vc/link-term)
                           (interpose " "))]
          [:<> {:key pinyin*}
           [:dt.pinyin pinyin*]
           [:dd
            [:ol
             (for [definition (no-fake-variants script term definitions)]
               [:li {:key definition}
                (let [link (comp vc/link-term vector)]
                  (vc/handle-refs script link definition))])]]]))]]))

(defn details-table
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
    [:section.details
     [:table
      [:tbody
       [:tr {:key   ::d/frequency
             :title "Word frequency"}
        [:td "Frequency"]
        [:td (cond
               (= label :high) "frequent"
               (= label :medium) "average"
               (= label :low) "infrequent")]]
       (when entry-script
         [:tr {:key   ::d/variations
               :title (str (if (= ::d/traditional entry-script)
                             "In Traditional Chinese"
                             "In Simplified Chinese"))}
          (if (= entry-script ::d/traditional)
            [:td "Traditional"]
            [:td "Simplified"])
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
          [:td "Classifiers"]
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
          [:td "Radical"]
          (if (= term radical)
            [:td "The character is a radical"]
            [:td {:lang zh} (vc/link-term (vector radical))])])
       (when etymology
         (let [{type     ::d/type
                hint     ::d/hint
                semantic ::d/semantic
                phonetic ::d/phonetic} etymology]
           (when-let [etym (cond
                             (and (or (= type "pictographic")
                                      (= type "ideographic")) hint)
                             [:<> (let [link (comp vc/link-term vector)]
                                    (vc/handle-refs script link hint))]

                             (and (= type "pictophonetic") semantic phonetic)
                             [:<>
                              [:span {:lang zh} (vc/link-term semantic)]
                              " (" hint ") + "
                              [:span {:lang zh} (vc/link-term phonetic)]])]
             [:tr {:key   ::d/etymology
                   :title "Etymology"}
              [:td (str/capitalize type)]                   ;TODO: do this during compilation?
              [:td etym]])))]]]))

(defn entry
  "Dictionary entry for a specific term."
  []
  [:main
   [:article.entry.full
    [entry-title]
    [:div.content
     [usage-list]
     [details-table]]]])

(defn- result-entry-uses
  "Listed uses of a search result entry."
  [script term uses]
  (for [[pronunciation definitions] uses]
    (when (not (empty? definitions))
      (let [handle-refs* (partial vc/handle-refs script identity)
            definitions* (->> definitions
                              (no-fake-variants script term)
                              (map handle-refs*))]
        [:<> {:key pronunciation}
         [:dt.pinyin
          (p/digits->diacritics pronunciation)]
         [:dd
          (interpose "; " definitions*)]]))))

(defn- search-result-entry
  "Entry in a results-list."
  [script
   {term ::d/term
    uses ::d/uses}]
  (when-let [entry-uses (result-entry-uses script term uses)]
    [:article {:key term}
     [:a {:href (str "/" (name ::d/terms) "/" term)}
      [:h1 {:lang (vc/zh script)}
       term]
      [:dl
       entry-uses]]]))

(defn search-results
  "List of search result entries."
  []
  (let [script             @(rf/subscribe [::subs/script])
        content            @(rf/subscribe [::subs/content])
        result-filter      @(rf/subscribe [::subs/current-result-filter])
        in-current-script? #(contains? (::d/scripts %) script)]
    (when-let [entries (get content result-filter)]
      [:main.entries
       (->> entries
            (filter in-current-script?)
            (map (partial search-result-entry script)))])))

(defn unknown-term
  "Dictionary entry for a term that does not exist."
  []
  [:main
   [:article.full
    [:h1 "Sorry,"]
    [:p "but here are no dictionary entries available matching that term."]]])

(defn dictionary-page
  "A dictionary page can be 1 of 3 types: entry, search result, or unknown."
  []
  (let [content @(rf/subscribe [::subs/content])]
    (cond
      (::d/uses content) [entry]
      (> (count (keys content)) 1) [search-results]
      :else [unknown-term])))
