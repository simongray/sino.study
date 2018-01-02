(ns sinostudy.pinyin.patterns
  (:require #?(:clj [clojure.spec.alpha :as spec]
               :cljs [cljs.spec.alpha :as spec])
            [clojure.string :as str]
            [sinostudy.pinyin.core :as core]
            [sinostudy.pinyin.data :as data]))

;; reverse-sorting the list of syllables prevents eager resolution in JS regex
;; otherwise syllables like "wang" will not match (they eagerly resolve to "wa")
(def ^:private rev-syllables
  (reverse (sort data/syllables)))

(def ^:private syllable
  (str "(" (str/join "|" rev-syllables) ")"))

(def ^:private syllable+digit
  (str "((" (str/join "|" rev-syllables) ")[012345]?)"))

(def ^:private block
  (let [syllable+ (str syllable "+")
        syllable* (str "('?" syllable ")*")]
    (str "(" syllable+ syllable* ")")))

(def ^:private block+digit
  (let [syllable+digit+ (str syllable+digit "+")
        syllable+digit* (str "('?" syllable+digit ")*")]
    (str "(" syllable+digit+ syllable+digit* ")")))

;; note: technically matches non-Latin, e.g. also matches hanzi
(def ^:private punct
  "[^\\w]+")

(def ^:private pinyin-pattern
  (re-pattern (str "(?i)" block)))

(def ^:private pinyin+punct-pattern
  (re-pattern (str "(?i)" block "(" block "|" punct ")*")))

(def ^:private pinyin+digits-pattern
  (re-pattern (str "(?i)" block+digit)))

(def ^:private pinyin+digits+punct-pattern
  (re-pattern (str "(?i)" block+digit "(" block+digit "|" punct ")*")))

(defn pinyin-syllable?
  "Is this a single Pinyin syllable (no digits or diacritics allowed)?"
  [s]
  #(data/syllables (str/lower-case %)))

(defn pinyin-block?
  "Is this a plain block of Pinyin (no digits or diacritics allowed)?"
  [s]
  (re-matches pinyin-pattern s))

(defn pinyin+punct?
  "Is this a sentence containing Pinyin without any tone digits or diacritics?"
  [s]
  (re-matches pinyin+punct-pattern s))

(defn pinyin-block+digits?
  "Is this a block of Pinyin with tone digits?"
  [s]
  (re-matches pinyin+digits-pattern s))

(defn pinyin+digits+punct?
  "Is this a sentence containing Pinyin with tone digits?"
  [s]
  (re-matches pinyin+digits+punct-pattern s))

(defn pinyin-block+diacritics?
  "Is this a block of Pinyin with tone diacritics?
   Note that this function does not validate the *placement* of diacritics!"
  [s]
  (pinyin-block? (core/no-diacritics s)))

(defn pinyin+diacritics+punct?
  "Is this a sentence containing Pinyin with tone diacritics?
   Note that this function does not validate the *placement* of diacritics!"
  [s]
  (pinyin+punct? (core/no-diacritics s)))

(spec/def ::pinyin-syllable pinyin-syllable?)

(spec/def ::pinyin-block pinyin-block?)

(spec/def ::pinyin-block+digits pinyin-block+digits?)

(spec/def ::pinyin+digits+punct pinyin+digits+punct?)

(spec/def ::pinyin-block+diacritics pinyin-block+diacritics?)

(spec/def ::pinyin+diacritics+punct pinyin+diacritics+punct?)
