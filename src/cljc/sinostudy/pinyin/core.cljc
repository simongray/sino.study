(ns sinostudy.pinyin.core
  (:require [clojure.string :as str]
    #?(:clj [clojure.spec.alpha :as spec]
       :cljs [cljs.spec.alpha :as spec])
      [sinostudy.pinyin.data :as data]))

(defn parse-int
  "Parses a string s into an integer."
  [s]
  #?(:clj  (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn umlaut
  "Replace the common substitute letter V in s with the proper Pinyin Ü."
  [s]
  (-> s
      (str/replace \v \ü)
      (str/replace \V \Ü)))

(defn diacritic
  "Get the diacriticised char based on Pinyin tone (0 through 5)."
  [char tone]
  (nth (data/diacritics char) tone))

;; derived from this guideline: http://www.pinyin.info/rules/where.html
(defn diacritic-index
  "Get the index in s where a diacritic should be put according to Pinyin rules;
  s is a Pinyin syllable with/without an affixed digit (e.g. wang2 or lao)."
  [s]
  (if (string? s)
    (let [s* (re-find #"[^\d]+" (str/lower-case s))]
      (cond
        (empty? s*) nil
        (str/includes? s* "a") (str/index-of s* "a")
        (str/includes? s* "e") (str/index-of s* "e")
        (str/includes? s* "ou") (str/index-of s* "o")
        :else (if-let [index (str/last-index-of s* "n")]
                (- index 1)
                (- (count s*) 1))))
    nil))

(defn digit->diacritic
  "Convert a Pinyin syllable/final s with an affixed tone digit into one with a
  tone diacritic. When converting more than a single syllable at a time,
  use digits->diacritics instead!"
  [s]
  (let [tone           (parse-int (str (last s)))
        s*             (subs s 0 (dec (count s)))
        char           (nth s (diacritic-index s))
        char+diacritic (diacritic char tone)]
    (str/replace s* char char+diacritic)))

;; used by diacritic-string to find the bounds of the last Pinyin final
(defn- last-final
  "Take a string with a single affixed tone digit as input and returns the
  longest allowed Pinyin final + the digit. The Pinyin final that is returned
  is the one immediately before the digit, i.e. the last final."
  [s]
  (let [digit  (last s)
        end    (dec (count s))           ; decrementing b/c of affixed digit
        length (if (< end 4) end 4)      ; most cases will be <4
        start  (- end length)]
    (loop [candidate (subs s start end)]
      (cond
        (empty? candidate) nil
        (contains? data/finals (str/lower-case candidate)) (str candidate digit)
        :else (recur (apply str (rest candidate)))))))

;; used by digits->diacritics to convert tone digits into diacritics
(defn- diacritic-string
  "Take a string with a single affixed tone digit as input and substitutes the
  digit with a tone diacritic. The diacritic is placed in the Pinyin final
  immediately before tone digit."
  [s]
  (let [final           (last-final s)
        final+diacritic (digit->diacritic final)
        ;; prefix = preceding neutral tone syllables + the initial
        prefix          (subs s 0 (- (count s) (count final)))]
    (str prefix final+diacritic)))

(defn digits->diacritics
  "Convert a Pinyin string s with one or several tone digits into a string with
  tone diacritics. The digits 0, 1, 2, 3, 4, and 5 can be used as tone markers
  behind any Pinyin final in the block. Postfixing 0 or 5 (or nothing) will
  result in no diacritic being added, i.e. marking a neutral tone. Furthermore,
  any occurrence of V is treated as and implicitly converted into a Ü."
  [s & {:keys [v-as-umlaut] :or {v-as-umlaut true} :as opts}]
  (if (string? s)
    (let [s*                (if v-as-umlaut (umlaut s) s)
          digit-strings     (re-seq #"[^\d]+\d" s*)
          diacritic-strings (map diacritic-string digit-strings)
          suffix            (re-seq #"[^\d]+$" s*)]
      (apply str (concat diacritic-strings suffix)))
    nil))

;; used by the pinyin+diacritics? (allows for evaluation as plain Pinyin)
(defn no-diacritics
  "Replace those characters in the input string s that have Pinyin diacritics
  with standard characters."
  ([s] (no-diacritics s data/diacritic-patterns))
  ([s [[replacement match] & xs]]
   (if (nil? match)
     s
     (recur (str/replace s match replacement) xs))))

;; reverse-sorting the list of syllables prevents eager resolution in JS regex
; otherwise, syllables like "wang" will not match (they eagerly resolve as "wa")
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
  (pinyin-block? (no-diacritics s)))

(defn pinyin+diacritics+punct?
  "Is this a sentence containing Pinyin with tone diacritics?
   Note that this function does not validate the *placement* of diacritics!"
  [s]
  (pinyin+punct? (no-diacritics s)))

(spec/def ::pinyin-syllable pinyin-syllable?)

(spec/def ::pinyin-block pinyin-block?)

(spec/def ::pinyin-block+digits pinyin-block+digits?)

(spec/def ::pinyin+digits+punct pinyin+digits+punct?)

(spec/def ::pinyin-block+diacritics pinyin-block+diacritics?)

(spec/def ::pinyin+diacritics+punct pinyin+diacritics+punct?)
