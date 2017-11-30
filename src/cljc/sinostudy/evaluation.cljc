(ns sinostudy.evaluation
  (:require [sinostudy.pinyin.core :refer [pinyin+punct? pinyin+digits+punct?]]))

;; cljsjs/xregexp doesn't include the extensions allowing for \p{Script=Han}
;; will just use this to generate suitable regex used in both clj and cljs:
;;     http://kourge.net/projects/regexp-unicode-block

;; pinyin sentences with tone digits can converted to diacritics,
;; but the action shouldn't appear if the sentence contains no tone digits!
(defn- digits->diacritics?
  [query]
  (and (pinyin+digits+punct? query)
       (not (pinyin+punct? query))))

(defn eval-query
  "Evaluate a query string to get a vector of possible actions."
  [query]
  (cond-> []
          (digits->diacritics? query) (conj :digits->diacritics)))
