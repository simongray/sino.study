(ns sinostudy.views.common
  (:require [sinostudy.pages.defaults :as pd]
            [sinostudy.pinyin.eval :as pe]
            [clojure.string :as str]
            [sinostudy.rim.core :as rim]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.embed :as embed]
            [sinostudy.pinyin.core :as p]))

(defn link-term
  "Add links to dictionary look-ups for each term in text.
  If text is a string, then each character is linked.
  If text is a collection (e.g. hiccup), then each collection item is linked."
  [text]
  (let [ids  (range (count text))
        link (fn [term id] [:a
                            {:title (str "look up " term)
                             :href  (str "/" (name pd/terms) "/" term)
                             :key   (str term "-" id)}
                            term])]
    (map link text ids)))

(defn hanzi-link
  "Link the text, but only link if the text is Hanzi."
  [text]
  (if (pe/hanzi-block? text)
    (link-term text)
    text))

(defn refr->m
  "Transform the embedded reference string into a Clojure map."
  [refr]
  (let [[hanzi-str pinyin-str] (str/split refr #"\[|\]")
        hanzi       (str/split hanzi-str #"\|")
        pinyin      (->> (str/split pinyin-str #" ")
                         (map p/digits->diacritics))
        traditional (first hanzi)
        simplified  (if (second hanzi) (second hanzi) traditional)]
    {::d/traditional traditional
     ::d/simplified  simplified
     ::d/pinyin      pinyin}))

(defn- link-reference
  "Link s if s is a reference. Helper function for `link-references."
  [script s]
  (let [link       (comp link-term vector)
        use-script (fn [coll]
                     (get coll (cond
                                 (= (count coll) 1) 0
                                 (= script ::d/simplified) 1
                                 :else 0)))]
    (cond
      (re-matches embed/refr s) (let [m      (refr->m s)
                                      pinyin (->> (::d/pinyin m)
                                                  (map link)
                                                  (interpose " "))]
                                  [:span
                                   [:span.hanzi (link (script m))]
                                   [:span.pinyin pinyin]])

      (re-matches embed/hanzi s) [:span.hanzi (link (-> s
                                                        (str/split #"\|")
                                                        use-script))]

      (pe/hanzi-block? s) [:span.hanzi (link s)]

      ;; TODO: do this properly when I find an example
      (re-matches embed/pinyin s) [:span.pinyin (link s)]

      ;; TODO: don't link numbers? i.e. 118 in "Kangxi radical 118"
      :else (link s))))

(defn link-references
  "Add hyperlink and style any references to dictionary entries in s.
  Script is the preferred script, i.e. traditional or simplified."
  [script s]
  ;; The part before the | matches the full embedded refs while the latter
  ;; part matches all non-space/non-punctuation items, e.g. words, hanzi.
  (let [non-ref #"[^\s]+\[[^\]]+\]|[^,.;'\"`´+?&()#%\s]+"
        link*   (partial link-reference script)]
    (rim/re-handle s non-ref link*)))
