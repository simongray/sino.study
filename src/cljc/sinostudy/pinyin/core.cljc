(ns sinostudy.pinyin.core
  (:require [clojure.string :as str]
            [sinostudy.rim.core :as rim]
            [sinostudy.pinyin.patterns :as patterns]
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
  (let [s* (re-find #"[^\d]+" (str/lower-case s))]
    (cond
      (not (string? s)) nil
      (empty? s*) nil
      (str/includes? s* "a") (str/index-of s* "a")
      (str/includes? s* "e") (str/index-of s* "e")
      (str/includes? s* "ou") (str/index-of s* "o")
      :else (if-let [index (str/last-index-of s* "n")]
              (- index 1)
              (- (count s*) 1)))))

(defn handle-m
  "Handle the super rare, special case final, m."
  [s]
  (let [tone (parse-int (str (last s)))
        skip (if (= \M (first s)) 6 0)]
    (nth data/m-diacritics (+ tone skip))))

(defn digit->diacritic
  "Convert a Pinyin syllable/final s with an affixed tone digit into one with a
  tone diacritic. When converting more than a single syllable at a time,
  use digits->diacritics instead!"
  [s]
  (cond
    (or (empty? s) (nil? s)) s
    (re-matches #"[mM]\d" s) (handle-m s)
    :else (let [tone           (parse-int (str (last s)))
                s*             (subs s 0 (dec (count s)))
                char           (nth s (diacritic-index s))
                char+diacritic (diacritic char tone)]
            (str/replace s* char char+diacritic))))

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

(defn- handle-r
  "Handle the common special case final, r."
  [s]
  (str/replace s #"\d" ""))

;; used by digits->diacritics to convert tone digits into diacritics
(defn- diacritic-string
  "Take a string with a single affixed tone digit as input and substitutes the
  digit with a tone diacritic. The diacritic is placed in the Pinyin final
  immediately before tone digit."
  [s]
  (if (contains? #{"r5" "R5" "r0" "R0"} (str/trim s))
    (handle-r s)
    (let [final           (last-final s)
          final+diacritic (digit->diacritic final)
          ;; prefix = preceding neutral tone syllables + the initial
          prefix          (subs s 0 (- (count s) (count final)))]
      (str prefix final+diacritic))))

(defn digits->diacritics
  "Convert a Pinyin string s with one or several tone digits into a string with
  tone diacritics. The digits 0, 1, 2, 3, 4, and 5 can be used as tone markers
  behind any Pinyin final in the block. Postfixing 0 or 5 (or nothing) will
  result in no diacritic being added, i.e. marking a neutral tone. Furthermore,
  any occurrence of V is treated as and implicitly converted into a Ü."
  [s & {:keys [v-as-umlaut] :or {v-as-umlaut false}}]
  (if (not (string? s))
    s
    (let [s*                (if v-as-umlaut (umlaut s) s)
          digit-strings     (re-seq #"[^\d]+\d" s*)
          diacritic-strings (map diacritic-string digit-strings)
          suffix            (re-seq #"[^\d]+$" s*)]
      (apply str (concat diacritic-strings suffix)))))

;; used by the pinyin+diacritics? (allows for evaluation as plain Pinyin)
(defn no-diacritics
  "Replace those characters in the input string s that have Pinyin diacritics
  with standard characters."
  ([s] (no-diacritics s data/diacritic-patterns))
  ([s [[replacement match] & xs]]
   (if (nil? match)
     s
     (recur (str/replace s match replacement) xs))))

(defn no-digits
  "Remove digits from the input string."
  [s]
  (str/replace s #"[0-9]" ""))

(defn- char->tone
  "Get the tone (0-4) based on a char."
  [char]
  (loop [tone 1]
    (cond
      (or (= nil char) (= 5 tone)) 0
      (re-matches (get data/tone-diacritics tone) char) tone
      :else (recur (inc tone)))))

(defn- replace-at
  "Like clojure.string/replace, but replaces between index from and to (excl)."
  [s from to replacement]
  (str (subs s 0 from) replacement (subs s to)))

(defn- diacritics->digits*
  "Replaces in s based on a replacements vector."
  [s replacements]
  (loop [skip          0
         s*            s
         replacements* replacements]
    (if-let [[from syllable tone] (first replacements*)]
      (recur (if tone (inc skip) skip)
             (replace-at s*
                         (+ skip from)
                         (+ skip from (count syllable))
                         (str syllable tone))
             (rest replacements*))
      s*)))

(defn diacritics->digits
  "Convert a Pinyin string s with tone diacritics into one with tone digits."
  [s]
  (let [s*        (no-diacritics s)
        syllables (rim/re-pos patterns/pinyin-syllable s*)
        original  #(subs s (first %) (+ (first %) (count (second %))))
        diacritic #(re-find #"[^\w]" %)
        tone      (comp #(if (= 0 %) nil %) char->tone diacritic original)]
    (diacritics->digits* s (map (juxt first second tone) syllables))))
