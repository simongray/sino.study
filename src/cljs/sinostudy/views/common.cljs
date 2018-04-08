(ns sinostudy.views.common
  (:require [sinostudy.pages.defaults :as pd]))

(defn link-term
  "Add links to dictionary look-ups for each term in text.
  If text is a string, then each character is linked.
  If text is a collection (e.g. hiccup), then each collection item is linked."
  [text]
  (let [ids  (range (count text))
        link (fn [term id] [:a {:title (str "look up " term)
                                :href  (str "/" (name pd/terms) "/" term)
                                :key   id} term])]
    (map link text ids)))