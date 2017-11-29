(ns sinostudy.evaluation)

;; cljsjs/xregexp doesnt include the extensions allowing for \p{Script=Han}
;; will just use this to generate suitable regex used in both clj and cljs:
;;     http://kourge.net/projects/regexp-unicode-block)


; TODO: use (cond-> s ...)
(defn evaluate-query
  "Evaluates a piece of input text and returns a vector of possible actions."
  [s]
  (cond
    (= s "") []
    (= s "bad") []
    :else [:analyse-text]))
