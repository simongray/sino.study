(ns sinostudy.pinyin.patterns
  (:require [clojure.string :as str]
            [sinostudy.pinyin.data :as data]))

;; reverse-sorting the list of syllables prevents eager resolution in JS regex
;; otherwise syllables like "wang" will not match (they eagerly resolve to "wa")
(def rev-syllables
  (reverse (sort data/syllables)))

;; This crazy concoction is used to validate Pinyin such as "hanguo".
;; If only checking front to back, it's read as "hang" + "uo", i.e. invalid.
;; By also validating the block in reverse, we get around this issue.
(def rev-rev-syllables
  (reverse (map #(str/join (reverse %)) (sort data/syllables))))

(def syllable
  (str "(" (str/join "|" rev-syllables) ")"))

(def rev-syllable
  (str "(" (str/join "|" rev-rev-syllables) ")"))

(def syllable+digit
  (str "((" (str/join "|" rev-syllables) ")[012345]?)"))

(def block
  (let [syllable+ (str syllable "+")
        syllable* (str "('?" syllable ")*")]
    (str "(" syllable+ syllable* ")")))

(def rev-block
  (let [syllable+ (str rev-syllable "+")
        syllable* (str "('?" rev-syllable ")*")]
    (str "(" syllable+ syllable* ")")))

(def block+digit
  (let [syllable+digit+ (str syllable+digit "+")
        syllable+digit* (str "('?" syllable+digit ")*")]
    (str "(" syllable+digit+ syllable+digit* ")")))

;; note: technically matches non-Latin, e.g. also matches hanzi
(def punct
  "[^\\w]+")

(def pinyin-syllable
  (re-pattern (str "(?i)" syllable)))

(def pinyin-block
  (re-pattern (str "(?i)" block)))

(def pinyin-rev-block
  (re-pattern (str "(?i)" rev-block)))

(def pinyin+punct
  (re-pattern (str "(?i)" block "(" block "|" punct ")*")))

(def pinyin+digits
  (re-pattern (str "(?i)" block+digit)))

(def pinyin+digits+punct
  (re-pattern (str "(?i)" block+digit "(" block+digit "|" punct ")*")))

;; from http://kourge.net/projects/regexp-unicode-block
(def hanzi-unicode
  {"CJK Radicals Supplement"            #"\u2E80-\u2EFF"
   "Kangxi Radicals"                    #"\u2F00-\u2FDF"
   "Ideographic Description Characters" #"\u2FF0-\u2FFF"
   "CJK Symbols and Punctuation"        #"\u3000-\u303F"
   "CJK Strokes"                        #"\u31C0-\u31EF"
   "Enclosed CJK Letters and Months"    #"\u3200-\u32FF"
   "CJK Compatibility"                  #"\u3300-\u33FF"
   "CJK Unified Ideographs Extension A" #"\u3400-\u4DBF"
   "Yijing Hexagram Symbols"            #"\u4DC0-\u4DFF"
   "CJK Unified Ideographs"             #"\u4E00-\u9FFF"
   "CJK Compatibility Ideographs"       #"\uF900-\uFAFF"
   "CJK Compatibility Forms"            #"\uFE30-\uFE4F"})

(def hanzi-block
  (re-pattern (str "[" (str/join (map str (vals hanzi-unicode))) "]+")))
