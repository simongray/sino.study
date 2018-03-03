(ns sinostudy.dictionary.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [sinostudy.dictionary.data :as data]
            [sinostudy.dictionary.embed :as embed]))

;; TODO: make list of exceptional entries (e.g. 3P) that should be queryable
;; TODO: make the pattern "classifier for X" prominent (CL)
;; TODO: make the pattern "to X" prominent (V)
;; TODO: tag radicals, e.g. def = "Kangxi radical 206" or just from a list

(def defs
  ::definitions)

(def pinyin
  ::pinyin)

(def pinyin-key
  ::pinyin-key)

(def pinyin+digits-key
  ::pinyin+digits-key)

(def pinyin+diacritics-key
  ::pinyin+diacritics-key)

(def simp
  ::simplified)

(def trad
  ::traditional)

(def cls
  ::classifiers)


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
  (let [script-diff? (not= (::traditional listing) (::simplified listing))
        make-vars    (fn [script]
                       (let [other (case script
                                     ::traditional ::simplified
                                     ::simplified ::traditional)]
                         {other #{(get listing other)}}))
        classifiers  (::classifiers listing)
        base-entry   {::word    (get listing script)
                      ::scripts #{script}
                      ::uses    {(::pinyin listing) (::definitions listing)}}]
    (cond-> base-entry
            script-diff? (assoc ::variations (make-vars script))
            classifiers (assoc ::classifiers classifiers))))

(defn hanzi-add*
  "Update the hanzi dict at the specified key k with the entry v.
  The entry is either inserted as is or merged with the old entry."
  [dict k v]
  (if-let [old (get dict k)]
    (let [scripts (set/union (::scripts old) (::scripts v))
          cls     (set/union (::classifiers old) (::classifiers v))
          uses    (merge-with set/union (::uses old) (::uses v))
          vars    (merge-with set/union (::variations old) (::variations v))]
      (assoc dict k (cond-> old
                            scripts (assoc ::scripts scripts)
                            cls (assoc ::classifiers cls)
                            uses (assoc ::uses uses)
                            vars (assoc ::variations vars))))
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
  (let [stopwords* (set/difference data/stopwords (::definitions listing))]
    (set/difference (->> (::definitions listing)
                         (map ^String str/lower-case)
                         (map #(str/split % #"[^a-z-]+"))
                         (flatten)
                         (set))
                    stopwords*)))

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


;;;;; CREATING DICTS AND LOOKING UP WORDS

(defn create-dicts
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps.
  The listings convert into multiple dictionary entries based on look-up type."
  [listings]
  (let [listings*                 (map detach-cls listings)
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
  (let [look-up*   (fn [dict word] (get (get dicts dict) word))
        get-entry  (fn [word] (look-up* ::hanzi word))
        hanzi      (get-entry word)
        english    (look-up* ::english word)
        pinyin     (look-up* ::pinyin word)
        digits     (look-up* ::pinyin+digits word)
        diacritics (look-up* ::pinyin+diacritics word)]
    (cond-> #{}
            hanzi (conj hanzi)
            english (set/union (set (map get-entry english)))
            pinyin (set/union (set (map get-entry pinyin)))
            digits (set/union (set (map get-entry digits)))
            diacritics (set/union (set (map get-entry diacritics))))))
