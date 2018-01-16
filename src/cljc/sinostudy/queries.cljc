(ns sinostudy.queries
  (:require [clojure.string :as str]
            [sinostudy.dictionary.core :as dict]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]))

;; cljsjs/xregexp doesn't include the extensions allowing for \p{Script=Han}
;; will just use this to generate suitable regex used in both clj and cljs:
;;     http://kourge.net/projects/regexp-unicode-block

;; pinyin sentences with tone digits can converted to diacritics,
;; but the action shouldn't appear if the sentence contains no tone digits!
(defn- digits->diacritics?
  [query]
  (and (pe/pinyin+digits+punct? query)
       (not (pe/pinyin+punct? query))))

(defn- diacritics->digits?
  [query]
  (and (pe/pinyin+diacritics+punct? query)
       (not (pe/pinyin+punct? query))))

(defn- word?
  [query]
  (or (pe/hanzi-block? query)
      (pe/pinyin-block? query)))

(defn- command?
  [query]
  (str/starts-with? query "/"))

(defn- eval-command
  "Evaluate a command query to get a vector of possible actions."
  [query]
  (case (str/lower-case query)
    "/clear" [[:clear]]
    "/test" [[:test]]
    []))

(defn eval-pinyin
  "Evaluate a Pinyin query to get a vector of possible actions."
  [query]
  (cond-> []
          (digits->diacritics? query) (conj [:digits->diacritics])
          (diacritics->digits? query) (conj [:diacritics->digits])))

;; TODO: more intelligent pinyin lookups
;; TODO: use events directly, i.e. ::events/digits->diacritics?
(defn eval-query
  "Evaluate a query string to get a vector of possible actions."
  [query]
  ;; some tests need an umlaut'ed query
  (let [query* (p/umlaut query)]
    (cond
      (command? query) (eval-command query)
      (pe/hanzi-block? query) [[:look-up-word query]]
      (pe/pinyin-block? query) [[:look-up-word (dict/pinyin-key query)]]
      :else (eval-pinyin query*))))
