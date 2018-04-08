(ns sinostudy.views.dictionary
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pinyin.core :as p]
            [sinostudy.views.common :as vc]
            [sinostudy.subs :as subs]
            [sinostudy.events :as events]
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
  "Render the title of the term with links to characters -OR- decomposition
  into components if the term is a character."
  [term decomposition]
  (let [attribute      @(rf/subscribe [::subs/current-attribute])
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
  "Render etymology information about a specific character."
  [{type     ::d/type
    hint     ::d/hint
    semantic ::d/semantic
    phonetic ::d/phonetic}]
  (cond
    (and (or (= type "pictographic")
             (= type "ideographic"))
         hint)
    [:span.etymology
     {:title (str "Type: " type)}
     "\"" hint "\""]

    (and (= type "pictophonetic")
         semantic
         phonetic)
    [:span.etymology
     {:title (str "Type: " type)}
     [:span.hanzi (vc/link-term semantic)]
     " (" hint ") + "
     [:span.hanzi (vc/link-term phonetic)]]))

(defn frequency-tag
  "Render a tag with a frequency label based on a word frequency."
  [frequency]
  [:span.tag
   {:key   ::d/frequency
    :title "Word frequency"}
   "fr.|"
   (case (d/frequency-label frequency)
     :high [:span.frequency-high "high"]
     :medium [:span.frequency-medium "medium"]
     :low [:span.frequency-low "low"])])

(defn variations-tag
  "Render a tag with the variations of an entry in a given script."
  [script variations]
  [:span.tag
   {:key   (str script "-variations")
    :title (str (if (= ::d/traditional script)
                  "Traditional Chinese"
                  "Simplified Chinese"))}
   (if (= script ::d/traditional)
     "tr.|"
     "s.|")
   (interpose ", " (->> variations
                        script
                        (map vector)
                        (map vc/link-term)
                        (map (fn [variation]
                               [:span.hanzi {:key variation} variation]))))])

(defn classifiers-tag
  "Render a tag with the classifiers of an entry in a given script."
  [script classifiers]
  [:span.tag {:key   ::d/classifiers
              :title (str "Common classifiers")}
   "cl.|"
   (interpose ", "
     (for [classifier (sort-by ::d/pinyin classifiers)]
       [:span.hanzi
        {:key (script classifier)}
        (vc/link-term (vector (script classifier)))]))])

(defn radical-tag
  "Renders a tag with the radical of a Hanzi."
  [term radical]
  [:span.tag {:key   ::d/radical
              :title (if (= term radical)
                       (str "The character is itself a radical")
                       (str "Radical"))}
   "rad.|"
   [:span.hanzi (if (= term radical)
                  radical
                  (vc/link-term (vector radical)))]])

(defn usage-list
  "Render a list of definitions for each Pinyin variation of an entry."
  [script uses]
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
            [:span.definition definition*]]))]])])

(defn entry
  "Render a dictionary entry for a specific term using the given script."
  [{term          ::d/term
    scripts       ::d/scripts
    uses          ::d/uses
    variations    ::d/variations
    frequency     ::d/frequency
    classifiers   ::d/classifiers
    radical       ::d/radical
    decomposition ::d/decomposition
    etymology     ::d/etymology}
   script]
  [:div.dictionary-entry
   [:h1
    (term-title term decomposition)
    (etymology-blurb etymology)]
   [:p.subheader
    (interpose " "
      (concat [(when frequency
                 (frequency-tag frequency))
               (when (::d/traditional variations)
                 (variations-tag ::d/traditional variations))
               (when (::d/simplified variations)
                 (variations-tag ::d/simplified variations))
               (when classifiers
                 (classifiers-tag script classifiers))
               (when radical
                 (radical-tag term radical))]))]
   (usage-list script uses)])

(defn search-result
  [search-result script]
  [:p "list result"])

(defn unknown-term
  "Render a dictionary entry for a term that does not exist."
  [term]
  [:h1 "Unknown term"
   [:p "There are no dictionary entries for the term \"" term "\"."]])

(defn dictionary-page
  "Render a dictionary page (entry or search result list)."
  [content script]
  (cond
    (::d/uses content) (entry content script)
    (> (count (keys content)) 1) (search-result content script)
    :else (unknown-term (::d/term content))))
