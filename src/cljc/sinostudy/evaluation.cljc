(ns sinostudy.evaluation)

; TODO: use (cond-> s ...)
(defn evaluate-query
  "Evaluates a piece of input text and returns a vector of possible actions."
  [s]
  (cond
    (= s "") []
    (= s "bad") []
    :else [:analyse-text]))
