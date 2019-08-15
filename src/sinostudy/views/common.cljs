(ns sinostudy.views.common
  (:require [sinostudy.navigation.pages :as pages]
            [sinostudy.events.scrolling :as scrolling]
            [sinostudy.pinyin.eval :as pe]
            [sinostudy.rim.core :as rim]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.embed :as embed]
            [sinostudy.pinyin.core :as p]
            [clojure.string :as str]
            [re-frame.core :as rf]))

;; The on-click handler that dispatches an event to reset the scroll state
;; is a necessity, given that it is currently not possible to distinguish
;; between back/forward button navigation events and clicking links.
;; Obviously, clicking a link should never result in a restored scroll state.
;; Similarly, some queries (e.g. look-ups) also manually reset the scroll state.
(defn link-term
  "Add links to dictionary look-ups for each term in text.
  If text is a string, then each character is linked.
  If text is a collection (e.g. hiccup), then each collection item is linked."
  [text]
  (let [ids  (range (count text))
        link (fn [term id]
               [:a
                {:title    (str "Look up " term)
                 :on-click #(rf/dispatch [::scrolling/reset-scroll-state
                                          [::pages/terms term]])
                 :href     (str "/" (name ::pages/terms) "/" term)
                 :key      (str term "-" id)}
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

(defn zh
  "Get the proper Chinese lang attribute based on the script."
  [script]
  (case script
    ::d/traditional "zh-Hant"
    ::d/simplified "zh-Hans"
    "zh"))

(defn- handle-ref
  "Handle s with f in the given script if s is a reference."
  [script f s]
  (let [zh         (zh script)
        use-script (fn [coll]
                     (get coll (cond
                                 (= (count coll) 1) 0
                                 (= script ::d/simplified) 1
                                 :else 0)))]
    (cond
      (re-matches embed/refr s) (let [m      (refr->m s)
                                      pinyin (->> (::d/pinyin m)
                                                  (map f)
                                                  (interpose " "))
                                      hanzi  (script m)]
                                  [:span {:key hanzi}
                                   [:span {:lang zh}
                                    (f hanzi)]
                                   [:span.pinyin
                                    pinyin]])

      (re-matches embed/hanzi s) (let [hanzi (-> s
                                                 (str/split #"\|")
                                                 (use-script))]
                                   [:span {:lang zh :key hanzi}
                                    (f hanzi)])

      (pe/hanzi-block? s) [:span {:lang zh
                                  :key  s}
                           (f s)]

      (re-matches embed/pinyin s) (let [pinyin (-> s
                                                   (subs 1 (dec (count s)))
                                                   (str/split #" "))]
                                    [:span.pinyin {:key s}
                                     (interpose " " (map f pinyin))])

      ;; TODO: don't link numbers? i.e. 118 in "Kangxi radical 118"
      :else (f s))))

(defn handle-refs
  "Add hyperlink and style any references to dictionary entries in s.
  Script is the preferred script, i.e. traditional or simplified."
  [script f s]
  ;; The part before the first | matches the full embedded refs;
  ;; The part before the second | part matches embedded pinyin;
  ;; The latter part matches all remaining words in English or Chinese.
  (let [non-ref     #"[^\s]+\[[^\]]+\]|\[[^\]]+\]|[^,.;'\"`´+?&()#%\s]+"
        handle-ref* (partial handle-ref script f)]
    (rim/re-handle s non-ref handle-ref*)))
