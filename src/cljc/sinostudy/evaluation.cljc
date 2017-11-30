(ns sinostudy.evaluation
  (:require [sinostudy.pinyin.core :refer [pinyin? pinyin+digits?]]))

;; cljsjs/xregexp doesn't include the extensions allowing for \p{Script=Han}
;; will just use this to generate suitable regex used in both clj and cljs:
;;     http://kourge.net/projects/regexp-unicode-block

(defn eval-query
  "Evaluate a query string to get a vector of possible actions."
  [query]
  (cond-> []
          ;; pinyin with tone digits can converted to diacritics
          (and (pinyin+digits? query) (not (pinyin? query)))
          (conj :digits->diacritics)))
