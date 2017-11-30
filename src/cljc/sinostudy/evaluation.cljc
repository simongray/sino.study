(ns sinostudy.evaluation
  (:require [sinostudy.pinyin.core :as pinyin]))

;; cljsjs/xregexp doesnt include the extensions allowing for \p{Script=Han}
;; will just use this to generate suitable regex used in both clj and cljs:
;;     http://kourge.net/projects/regexp-unicode-block

(defn evaluate-query
  "Evaluates a piece of input text and returns a vector of possible actions."
  [s]
  (cond-> []
          (pinyin/pinyin+digits? s) (conj :digits->diacritics)))
