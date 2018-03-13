(ns sinostudy.dictionary.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [sinostudy.dictionary.data :as data]
            [sinostudy.dictionary.embed :as embed]))

;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable
;; TODO: make the pattern "classifier for X" prominent (CL)
;; TODO: make the pattern "to X" prominent (V)
;; TODO: tag radicals, e.g. def = "Kangxi radical 206" or just from a list
;;       -- can just use: #(= (:word %) (:radical %))

(def defs
  ::definitions)

(def pinyin
  ::pinyin)

(def simp
  ::simplified)

(def trad
  ::traditional)

(def cls
  ::classifiers)


;;;; GENERAL STUFF

(defn pinyin-key
  "Convert a CC-CEDICT Pinyin string into a form for use as a map key."
  [s]
  (-> s
      (str/replace "'" "")
      (str/replace " " "")
      (str/replace "·" "")                                  ; middle dot
      (str/replace "," "")
      str/lower-case))


;;;; EMBEDDING MANIPULATION

(defn refr->m
  "Transform the embedded reference string into a Clojure map."
  [refr]
  (let [[hanzi-str pinyin-str] (str/split refr #"\[|\]")
        hanzi       (str/split hanzi-str #"\|")
        pinyin      (str/split pinyin-str #" ")
        traditional (first hanzi)
        simplified  (if (second hanzi) (second hanzi) traditional)]
    {::traditional traditional
     ::simplified  simplified
     ::pinyin      pinyin}))


;;;; DEALING WITH CLASSIFIERS

(defn cl-def?
  "Determine if a dictionary definition is actually a list of classifiers."
  [definition]
  (str/starts-with? definition "CL:"))

(defn has-cls?
  "Determine if the listing's ::definitions contain classifiers."
  [listing]
  (some cl-def? (::definitions listing)))

(defn detach-cls
  "Move the classifiers of a listing from ::definitions to ::classifiers."
  [listing]
  (if (has-cls? listing)
    (let [defs    (::definitions listing)
          cl-defs (filter cl-def? defs)
          get-cls (comp (partial map refr->m) (partial re-seq embed/refr))
          cls     (set (flatten (map get-cls cl-defs)))]
      (if cls
        (-> listing
            (assoc ::definitions (set/difference defs cl-defs))
            (assoc ::classifiers cls))
        listing))
    listing))


;;;; UNIFIED HANZI DICT (TRADITIONAL + SIMPLIFIED)

(defn hanzi-entry
  "Make a hanzi dictionary entry based on a script and a CC-CEDICT listing."
  [script listing]
  (let [script-diff?  (not= (::traditional listing) (::simplified listing))
        make-vars     (fn [script]
                        (let [other (case script
                                      ::traditional ::simplified
                                      ::simplified ::traditional)]
                          {other #{(get listing other)}}))
        classifiers   (::classifiers listing)
        frequency     (::frequency listing)
        decomposition (get-in listing (conj [::info script] ::decomposition))
        etymology     (get-in listing (conj [::info script] ::etymology))
        radical       (get-in listing (conj [::info script] ::radical))
        base-entry    {::word    (get listing script)
                       ::scripts #{script}
                       ::uses    {(::pinyin listing) (::definitions listing)}}]
    (cond-> base-entry
            script-diff? (assoc ::variations (make-vars script))
            classifiers (assoc ::classifiers classifiers)
            frequency (assoc ::frequency frequency)
            decomposition (assoc ::decomposition decomposition)
            etymology (assoc ::etymology etymology)
            radical (assoc ::radical radical))))

(defn hanzi-add*
  "Update the hanzi dict at the specified key k with the entry v.
  The entry is either inserted as is or merged with the old entry."
  [dict k v]
  (if-let [old (get dict k)]
    (let [scripts (set/union (::scripts old) (::scripts v))
          cls     (set/union (::classifiers old) (::classifiers v))
          uses    (merge-with set/union (::uses old) (::uses v))
          vars    (merge-with set/union (::variations old) (::variations v))
          freq    (::frequency v)
          decomp  (::decomposition v)
          etym    (::etymology v)
          radical (::radical v)]
      (assoc dict k (cond-> old
                            scripts (assoc ::scripts scripts)
                            cls (assoc ::classifiers cls)
                            uses (assoc ::uses uses)
                            vars (assoc ::variations vars)
                            freq (assoc ::frequency freq)
                            decomp (assoc ::decomposition decomp)
                            etym (assoc ::etymology etym)
                            radical (assoc ::radical radical))))
    (assoc dict k v)))

(defn hanzi-add
  "Add 1 to 2 entries in the hanzi dictionary from a CC-CEDICT listing."
  [dict listing]
  (-> dict
      (hanzi-add* (::traditional listing) (hanzi-entry ::traditional listing))
      (hanzi-add* (::simplified listing) (hanzi-entry ::simplified listing))))


;;;; PINYIN DICT

;; used by both pinyin-add and english-add
(defn generic-add
  "Add an entry to a dictionary; clashes are merged into a set."
  [dict k v]
  (if-let [old (get dict k)]
    (assoc dict k (set/union old v))
    (assoc dict k v)))

(defn pinyin-entry
  "Make a pinyin dictionary entry based on a CC-CEDICT listing."
  [listing]
  (conj #{(::traditional listing)} (::simplified listing)))

(defn pinyin-add
  "Add an entry to a pinyin dictionary from a CC-CEDICT listing."
  [key-type dict listing]
  (let [k (get listing key-type)
        v (pinyin-entry listing)]
    (generic-add dict k v)))


;;;; ENGLISH DICT

(defn english-keys
  "Find English dictionary keys based on a CC-CEDICT listing.
  Stop-words are removed entirely, unless they make up a full definition."
  [listing]
  (let [definitions  (::definitions listing)
        stopwords*   (set/difference data/stopwords definitions)
        single-words (->> definitions
                          (map ^String str/lower-case)
                          (map #(str/split % #"[^a-z-]+"))
                          (flatten)
                          (filter (comp not str/blank?))

                          (set))
        verblikes    (->> definitions
                          (filter #(str/starts-with? % "to "))
                          (map #(subs % 3)))
        keys         (set/union definitions
                                single-words
                                verblikes)]
    (set/difference keys stopwords*)))

(defn english-add
  "Add an entry to the pinyin dictionary from a CC-CEDICT listing."
  [dict listing]
  (let [ks (english-keys listing)
        v  (conj #{(::traditional listing)} (::simplified listing))]
    (loop [dict* dict
           ks*   ks]
      (if (seq ks*)
        (recur (generic-add dict* (first ks*) v) (rest ks*))
        dict*))))

(defn english-relevance
  "Calculate the relevance of entry based on an English word as the search term.
  The relevance is a score from 0 to ~1, higher being more relevant.
  Relevance is able to exceed 1 slightly, as word frequency is also added to the
  score, allowing for more accurate sorting (it is a number from 0 to 1 that
  tends towards 0). This is what puts e.g. 句子 ahead of 语句 for 'sentence'."
  [word entry]
  (let [uses      (apply set/union (vals (::uses entry)))
        score     (fn [use]
                    (if (str/includes? use word)
                      (/ (count word) (count use))
                      0))
        scores    (map score uses)
        max-score (apply max scores)
        freq      (get entry ::frequency 0)]
    ;; Note: multiple 0.0 scores only count as a single zero!
    ;; This is done to not unfairly weigh down words with many meanings.
    (+ max-score freq)))

(defn sort-by-english-relevance
  "Sort a list of entries based on their relevance to an english word."
  [word entries]
  ;; Memoized to reduce re-calculation of relevance scores
  ;; (sort-by expects a keyfn as that argument, i.e. O(0) is expected).
  (let [relevance (memoize (partial english-relevance word))]
    (sort-by relevance > entries)))


;;;; FREQUENCY DICTIONARY

(defn add-freq
  "Add word frequency (not char frequency) to a listing."
  [freq-dict listing]
  (let [trad-freq (get freq-dict (::traditional listing) 0)
        simp-freq (get freq-dict (::simplified listing) 0)
        frequency (max trad-freq simp-freq)]
    (if (> frequency 0)
      (assoc listing ::frequency frequency)
      listing)))


;;;; CHARACTER ETYMOLOGY, DECOMPOSITION, ETC.

(defn add-info*
  [script makemeahanzi listing]
  (if-let [info (get makemeahanzi (get listing script))]
    (let [decomposition (get info "decomposition")
          ;; TODO: is a tagged literal the proper way to prepend the ns?
          etymology     (if-let [raw (get info "etymology")]
                          (->> raw
                               (walk/keywordize-keys)
                               (tagged-literal 'sinostudy.dictionary.core))
                          nil)
          radical       (get info "radical")
          assoc*        (fn [coll k v]
                          (assoc-in coll (conj [::info script] k) v))]
      (cond-> listing
              decomposition (assoc* ::decomposition decomposition)
              etymology (assoc* ::etymology etymology)
              radical (assoc* ::radical radical)))
    listing))

(defn add-info
  [makemeahanzi listing]
  (->> listing
       (add-info* ::traditional makemeahanzi)
       (add-info* ::simplified makemeahanzi)))


;;;; CREATING DICTS AND LOOKING UP WORDS

;; TODO: also add listings only found in makemeahanzi (e.g. 忄)
(defn create-dicts
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps.
  The listings convert into multiple dictionary entries based on look-up type.
  A freq-dict is used to add the word frequency to each entry if available."
  [listings freq-dict makemeahanzi]
  (let [listings*                 (->> listings
                                       (map detach-cls)
                                       (map (partial add-freq freq-dict))
                                       (map (partial add-info makemeahanzi)))
        pinyin-key-add            (partial pinyin-add ::pinyin-key)
        pinyin+digits-key-add     (partial pinyin-add ::pinyin+digits-key)
        pinyin+diacritics-key-add (partial pinyin-add ::pinyin+diacritics-key)]
    {::hanzi             (reduce hanzi-add {} listings*)
     ::english           (reduce english-add {} listings*)
     ::pinyin            (reduce pinyin-key-add {} listings*)
     ::pinyin+digits     (reduce pinyin+digits-key-add {} listings*)
     ::pinyin+diacritics (reduce pinyin+diacritics-key-add {} listings*)}))

(defn look-up
  "Look up the specified word in each dictionary and merge the results."
  [dicts word]
  (let [look-up*    (fn [dict word] (get (get dicts dict) word))
        get-entries (fn [words] (set (map #(look-up* ::hanzi %) words)))
        hanzi       (look-up* ::hanzi word)
        pinyin      (look-up* ::pinyin word)
        digits      (look-up* ::pinyin+digits word)
        diacritics  (look-up* ::pinyin+diacritics word)
        english     (look-up* ::english word)]
    (cond-> {}
            hanzi (assoc ::hanzi #{hanzi})
            pinyin (assoc ::pinyin (get-entries pinyin))
            digits (assoc ::pinyin+digits (get-entries digits))
            diacritics (assoc ::pinyin+diacritics (get-entries diacritics))
            english (assoc ::english (get-entries english)))))