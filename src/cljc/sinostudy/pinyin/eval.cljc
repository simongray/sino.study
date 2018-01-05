(ns sinostudy.pinyin.eval
  (:require #?(:clj [clojure.spec.alpha :as spec]
               :cljs [cljs.spec.alpha :as spec])
              [clojure.string :as str]
              [sinostudy.pinyin.core :as core]
              [sinostudy.pinyin.data :as data]
              [sinostudy.pinyin.patterns :as patterns]))

(defn pinyin-syllable?
  "Is this a single Pinyin syllable (no digits or diacritics allowed)?"
  [s]
  (contains? data/syllables (str/lower-case s)))

(defn pinyin-block?
  "Is this a plain block of Pinyin (no digits or diacritics allowed)?"
  [s]
  (re-matches patterns/pinyin-pattern s))

(defn pinyin+punct?
  "Is this a sentence containing Pinyin without any tone digits or diacritics?"
  [s]
  (re-matches patterns/pinyin+punct-pattern s))

(defn pinyin-block+digits?
  "Is this a block of Pinyin with tone digits?"
  [s]
  (re-matches patterns/pinyin+digits-pattern s))

(defn pinyin+digits+punct?
  "Is this a sentence containing Pinyin with tone digits?"
  [s]
  (re-matches patterns/pinyin+digits+punct-pattern s))

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

(defn hanzi?
  [s]
  (re-matches patterns/hanzi-pattern s))

(spec/def ::pinyin-syllable pinyin-syllable?)

(spec/def ::pinyin-block pinyin-block?)

(spec/def ::pinyin-block+digits pinyin-block+digits?)

(spec/def ::pinyin+digits+punct pinyin+digits+punct?)

(spec/def ::pinyin-block+diacritics pinyin-block+diacritics?)

(spec/def ::pinyin+diacritics+punct pinyin+diacritics+punct?)
