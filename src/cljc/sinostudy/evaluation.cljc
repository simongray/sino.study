(ns sinostudy.evaluation
  (:require [clojure.string :as str]
            [sinostudy.pinyin.core :as pinyin]
            [sinostudy.pinyin.patterns :as patterns]))

;; cljsjs/xregexp doesn't include the extensions allowing for \p{Script=Han}
;; will just use this to generate suitable regex used in both clj and cljs:
;;     http://kourge.net/projects/regexp-unicode-block

;; pinyin sentences with tone digits can converted to diacritics,
;; but the action shouldn't appear if the sentence contains no tone digits!
(defn- digits->diacritics?
  [query]
  (and (patterns/pinyin+digits+punct? query)
       (not (patterns/pinyin+punct? query))))

(defn- hanzi?
  [query]
  (or (patterns/hanzi? query)
      (patterns/pinyin-block? query)))

(defn- command?
  [query]
  (str/starts-with? query "/"))

(defn- eval-command
  "Evaluate a command query to get a vector of possible actions."
  [query]
  (case (str/lower-case query)
    "/clear" [:clear]
    "/test" [:test]
    []))

(defn eval-pinyin
  "Evaluate a Pinyin query to get a vector of possible actions."
  [query]
  (cond-> []
          (patterns/pinyin-block? query) (conj :look-up-word)
          (digits->diacritics? query) (conj :digits->diacritics)))

(defn eval-query
  "Evaluate a query string to get a vector of possible actions."
  [query]
  ;; some tests need an umlaut'ed query
  (let [query* (pinyin/umlaut query)]
    (cond
      (command? query) (eval-command query)
      (hanzi? query) [:look-up-word]
      :else (eval-pinyin query*))))
