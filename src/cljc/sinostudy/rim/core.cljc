(ns sinostudy.rim.core
  (:require [clojure.string :as str]))

;;;; TEXT-RELATED FUNCTIONS

;; based on code examples from StackOverflow:
;; https://stackoverflow.com/questions/3262195/compact-clojure-code-for-regular-expression-matches-and-their-position-in-string
;; https://stackoverflow.com/questions/18735665/how-can-i-get-the-positions-of-regex-matches-in-clojurescript
(defn re-pos
  "Like re-seq, but returns a map of indexes to matches, not a seq of matches."
  [re s]
  #?(:clj  (loop [out {}
                  m   (re-matcher re s)]
             (if (.find m)
               (recur (assoc out (.start m) (.group m)) m)
               out))
     :cljs (let [flags (fn [re]
                         (let [m? (.-multiline re)
                               i? (.-ignoreCase re)]
                           (str "g" (when m? "m") (when i? "i"))))
                 re    (js/RegExp. (.-source re) (flags re))]
             (loop [out {}]
               (if-let [m (.exec re s)]
                 (recur (assoc out (.-index m) (first m)))
                 out)))))

(defn- re-handle*
  "Helper function for re-handle. Only takes strings."
  [s re f]
  (let [matches (re-seq re s)]
    (if (empty? matches)
      s
      (let [others  (str/split s re)
            ;; Dealing with weird behaviour present in Java/JS implementations
            ;; causing empty strings as the first split result.
            others* (if (= "" (first others))
                      (rest others)
                      others)
            results (map f matches)
            [c1 c2] (if (str/starts-with? s (first matches))
                      [results others*]
                      [others* results])
            c3      (if (> (count c1) (count c2))
                      (subvec (vec c1) (count c2))
                      (subvec (vec c2) (count c1)))]
        (concat (vec (interleave c1 c2)) c3)))))

(defn re-handle
  "Split s based on re and reinsert the matches of re in s with f applied.
  If s is sequential, then will apply f to matches inside any strings in s.
  Note: can be chained -- very useful for creating hiccup data out of a string."
  [s re f]
  (if (sequential? s)
    (map #(if (string? %) (re-handle* % re f) %) s)
    (re-handle* s re f)))
