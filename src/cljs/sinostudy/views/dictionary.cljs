(ns sinostudy.views.dictionary
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]
            [sinostudy.views.common :as vc]
            [sinostudy.subs :as subs]
            [sinostudy.rim.core :as rim]
            [sinostudy.dictionary.embed :as embed]
            [sinostudy.pages.defaults :as pd]))

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
          {:href (str "/" (name pd/terms) "/" term "/decomposition")}
          term]]

        :else
        [:span.hanzi
         {:title term}
         term]))))

;;; TODO: need to be able link hint text in 你 (ideographic)
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
            (vc/link-references script hint)]

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

;;; TODO: 只 has multiple identical definitions
(defn usage-list
  "List of definitions for each Pinyin variation of an entry."
  []
  (let [script @(rf/subscribe [::subs/script])
        {uses ::d/uses} @(rf/subscribe [::subs/content])]
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
         (for [definition definitions]
           [:li {:key definition}
            [:span.definition (vc/link-references script definition)]])]])]))

(defn entry
  "Dictionary entry for a specific term."
  []
  [:div.dictionary-entry
   [dictionary-header]
   [tags]
   [usage-list]])

(defn search-result
  "Dictionary search result."
  []
  [:p "list result"])

(defn unknown-term
  "Dictionary entry for a term that does not exist."
  [term]
  [:h1 "Unknown term"
   [:p "There are no dictionary entries for the term \"" term "\"."]])

(defn dictionary-page
  "A dictionary page can be 1 of 3 types: entry, search result, or unknown."
  []
  (let [content @(rf/subscribe [::subs/content])]
    (cond
      (::d/uses content) [entry]
      (> (count (keys content)) 1) [search-result]
      :else (unknown-term (::d/term content)))))
