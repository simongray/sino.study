(ns sinostudy.dictionary.embed
  (:require [clojure.string :as str]
            [sinostudy.pinyin.data :as pd]))

;;;; CC-CEDICT EMBEDDINGS

(def refr
  "A pattern used in CC-CEDICT to embed a hanzi reference with Pinyin."
  #"[^ ,:\[a-zA-Z0-9]+\[[^\]]+\]+")

;; CLJS regex seems to have some issues with doing (str pp/hanzi-pattern),
;; so I've copied over whole implementation.
(def hanzi
  "A pattern used in CC-CEDICT to embed a hanzi reference (no Pinyin)."
  (let [hanzi+ (str "[" (str/join (map str (vals pd/hanzi-unicode))) "]+")]
    (re-pattern (str hanzi+ "\\|?" hanzi+))))

(def pinyin
  "A pattern used in CC-CEDICT to embed a Pinyin pronunciation."
  #"\[[a-zA-Z0-9 ]+\]+")
