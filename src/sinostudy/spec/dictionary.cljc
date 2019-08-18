(ns sinostudy.spec.dictionary
  "Contains all specs pertaining to dictionary entries and search results."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; There's no built-in predicate for this.
(s/def ::non-blank-string
  (s/and string?
         (complement str/blank?)))

;; TODO: expand on this
(s/def ::hanzi
  ::non-blank-string)

;; TODO: expand on this
(s/def ::pinyin+digits
  ::non-blank-string)

(s/def ::term
  ::non-blank-string)

(s/def ::script
  #{:simplified
    :traditional})

(s/def ::scripts
  (s/coll-of ::script :kind set? :into #{}))

(s/def ::definition
  ::non-blank-string)

(s/def ::definitions
  (s/coll-of ::definition :kind set? :into #{}))

(s/def ::uses
  (s/map-of ::pinyin+digits ::definitions))

(s/def ::variations
  (s/map-of ::script (s/coll-of ::hanzi :kind set? :into #{})))

(s/def :classifier/traditional
  ::hanzi)

(s/def :classifier/simplified
  ::hanzi)

(s/def :classifier/pinyin
  (s/coll-of ::pinyin+digits))

(s/def ::classifier
  (s/keys :req-un [:classifier/traditional
                   :classifier/simplified
                   :classifier/pinyin]))

(s/def ::classifiers
  (s/coll-of ::classifier :kind set? :into #{}))

;; TODO: expand on this
(s/def ::decomposition
  string?)

(s/def ::frequency
  (s/double-in :min 0
               :max 1))

(s/def ::radical
  ::hanzi)

(s/def ::type
  #{"ideographic"
    "pictographic"
    "pictophonetic"})

(s/def ::phonetic
  ::hanzi)

(s/def ::semantic
  ::hanzi)

(s/def ::hint
  string?)

;; See: https://www.skishore.me/makemeahanzi/
(s/def ::etymology
  (s/keys :req-un [::type]
          :opt-un [::phonetic
                   ::semantic
                   ::hint]))

(s/def ::entry
  (s/keys :req-un [::term
                   ::scripts
                   ::uses]
          :opt-un [::radical
                   ::frequency
                   ::variations
                   ::classifiers
                   ::etymology]))

(s/def :search-result/hanzi
  ::entry)

(s/def :search-result/pinyin
  (s/coll-of ::entry))

(s/def :search-result/pinyin+digits
  (s/coll-of ::entry))

(s/def :search-result/pinyin+diacritics
  (s/coll-of ::entry))

(s/def :search-result/english
  (s/coll-of ::entry))

(s/def ::search-result
  (s/keys :req-un [::term]
          :opt-un [:search-result/hanzi
                   :search-result/pinyin
                   :search-result/pinyin+digits
                   :search-result/pinyin+diacritics
                   :search-result/english]))
