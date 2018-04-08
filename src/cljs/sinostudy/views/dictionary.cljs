(ns sinostudy.views.dictionary
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.core :as p]
            [sinostudy.views.common :as vc]
            [sinostudy.subs :as subs]
            [sinostudy.rim.core :as rim]
            [sinostudy.dictionary.embed :as embed]
            [sinostudy.pages.defaults :as pd]))

(defn hanzi-link
  "Link the text, but only link if the text is Hanzi."
  [text]
  (if (sinostudy.pinyin.eval/hanzi-block? text)
    (vc/link-term text)
    text))

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
         (map hanzi-link decomposition*)]

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
  (let [{etymology ::d/etymology} @(rf/subscribe [::subs/content])]
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
            hint]

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
  (let [{frequency ::d/frequency} @(rf/subscribe [::subs/content])]
    [:span.tag
     {:key   ::d/frequency
      :title "Word frequency"}
     (case (d/frequency-label frequency)
       :high [:span.frequency-high "frequent"]
       :medium [:span.frequency-medium "average"]
       :low [:span.frequency-low "infrequent"])]))

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
           ;; TODO: make less daunting
           (let [link        (comp vc/link-term vector)
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
             [:li {:key definition}
              [:span.definition definition*]]))]])]))

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
  "Dictionary page (dictionary entry, search results, or unknown term)."
  []
  (let [content @(rf/subscribe [::subs/content])]
    (cond
      (::d/uses content) [entry]
      (> (count (keys content)) 1) [search-result]
      :else (unknown-term (::d/term content)))))
