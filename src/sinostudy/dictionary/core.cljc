(ns sinostudy.dictionary.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [sinostudy.pinyin.core :as p]
            [sinostudy.dictionary.data :as data]
            [sinostudy.dictionary.embed :as embed]))

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
    {:traditional traditional
     :simplified  simplified
     :pinyin      pinyin}))


;;;; DEALING WITH CLASSIFIERS

(defn cl-def?
  "Determine if a dictionary definition is actually a list of classifiers."
  [definition]
  (str/starts-with? definition "CL:"))

(defn has-cls?
  "Determine if the listing's :definitions contain classifiers."
  [listing]
  (some cl-def? (:definitions listing)))

(defn detach-cls
  "Move the classifiers of a listing from :definitions to :classifiers."
  [listing]
  (if (has-cls? listing)
    (let [defs    (:definitions listing)
          cl-defs (filter cl-def? defs)
          get-cls (comp (partial map refr->m) (partial re-seq embed/refr))
          cls     (set (flatten (map get-cls cl-defs)))]
      (if cls
        (-> listing
            (assoc :definitions (set/difference defs cl-defs))
            (assoc :classifiers cls))
        listing))
    listing))


;;;; UNIFIED HANZI DICT (TRADITIONAL + SIMPLIFIED)

(defn hanzi-entry
  "Make a hanzi dictionary entry based on a script and a CC-CEDICT listing."
  [script listing]
  (let [script-diff?  (not= (:traditional listing) (:simplified listing))
        make-vars     (fn [script]
                        (let [other (case script
                                      :traditional :simplified
                                      :simplified :traditional)]
                          {other #{(get listing other)}}))
        classifiers   (:classifiers listing)
        frequency     (:frequency listing)
        decomposition (get-in listing [:info script :decomposition])
        etymology     (get-in listing [:info script :etymology])
        radical       (get-in listing [:info script :radical])
        base-entry    {:term    (get listing script)
                       :scripts #{script}
                       :uses    {(:pinyin listing) (:definitions listing)}}]
    (cond-> base-entry
            script-diff? (assoc :variations (make-vars script))
            classifiers (assoc :classifiers classifiers)
            frequency (assoc :frequency frequency)
            decomposition (assoc :decomposition decomposition)
            etymology (assoc :etymology etymology)
            radical (assoc :radical radical))))

(defn add-hanzi*
  "Update the hanzi dict at the specified key k with the entry v.
  The entry is either inserted as is or merged with the old entry."
  [dict k v]
  (if-let [old (get dict k)]
    (let [scripts (set/union (:scripts old) (:scripts v))
          cls     (set/union (:classifiers old) (:classifiers v))
          uses    (merge-with set/union (:uses old) (:uses v))
          vars    (merge-with set/union (:variations old) (:variations v))
          freq    (:frequency v)
          decomp  (:decomposition v)
          etym    (:etymology v)
          radical (:radical v)]
      (assoc dict k (cond-> old
                            scripts (assoc :scripts scripts)
                            cls (assoc :classifiers cls)
                            uses (assoc :uses uses)
                            vars (assoc :variations vars)
                            freq (assoc :frequency freq)
                            decomp (assoc :decomposition decomp)
                            etym (assoc :etymology etym)
                            radical (assoc :radical radical))))
    (assoc dict k v)))

(defn add-hanzi
  "Add 1 to 2 entries in the hanzi dictionary from a CC-CEDICT listing."
  [dict listing]
  (-> dict
      (add-hanzi* (:traditional listing) (hanzi-entry :traditional listing))
      (add-hanzi* (:simplified listing) (hanzi-entry :simplified listing))))


;;;; PINYIN DICT

;; used by both pinyin-add and english-add
(defn add
  "Add an entry to a dictionary; clashes are merged into a set."
  [dict k v]
  (if-let [old (get dict k)]
    (assoc dict k (set/union old v))
    (assoc dict k v)))

(defn pinyin-entry
  "Make a pinyin dictionary entry based on a CC-CEDICT listing."
  [listing]
  (hash-set (:traditional listing) (:simplified listing)))

(defn add-pinyin
  "Add an entry to a pinyin dictionary from a CC-CEDICT listing."
  [key-type dict listing]
  (let [k (get listing key-type)
        v (pinyin-entry listing)]
    (add dict k v)))


;;;; ENGLISH DICT

(defn remove-embedded
  "Removes embedded CC-CEDICT information from string s."
  [s]
  (-> s
      (str/replace embed/refr "")
      (str/replace embed/hanzi "")
      (str/replace embed/pinyin "")))

;; Explanatory parentheses, i.e. description preceding/following a definition.
(def expl
  #"^\([^)]+\)|\([^)]+\)$")

(defn english-keys
  "Find English dictionary keys based on a CC-CEDICT listing.
  Words inside explanatory parentheses are not considered.
  Numbers (unless they make up part of a word) are not considered.
  Stop-words are removed entirely, unless they make up a full definition
  or if they are part of a verblike, e.g. 'to have' or 'to laugh'."
  [definitions]
  (let [definitions* (->> definitions
                          (map #(str/replace %1 expl ""))
                          (map str/trim)
                          (map ^String str/lower-case)
                          (set))
        single-words (->> definitions*
                          (map remove-embedded)
                          (map #(str/split % #"[^a-z0-9-']+"))
                          (flatten)
                          (filter (comp not str/blank?))
                          (filter (comp not (partial re-find #"^[0-9]+$")))
                          (set))
        verblikes    (->> definitions*
                          (filter #(str/starts-with? % "to "))
                          (map #(subs % 3))
                          (set))
        stopwords*   (-> data/stopwords
                         (set/difference definitions*)
                         (set/difference verblikes))
        keys         (set/union definitions*
                                single-words
                                verblikes)]
    (set/difference keys stopwords*)))

;; Used on the backend for limiting results.
;; (Indirectly) used on the frontend when sorting results.
(defn- english-relevance-score
  "Calculates a basic relevance score based on the basic rule of term:use ratio
  as well as a few heuristics. All comparisons are done in lower case.

  Current heuristics:
    * ratio where explanatory parentheses are normalised to the same length: _
    * ratio with prefixed 'to ' removed (common marker of verblikes)"
  [term use]
  (let [to    #"^to "
        term* (str/lower-case term)
        use*  (str/lower-case use)]
    (if (str/includes? use* term*)
      (let [normalised-expl  "_"
            use-without-expl (str/replace use* expl normalised-expl)
            use-without-to   (str/replace use* to "")]
        (max
          ;; Basic ratio comparison
          (/ (count term*) (count use*))

          ;; Ratio comparison with explanatory parentheses normalised
          (if (and (str/includes? use-without-expl term*)
                   (not= use-without-expl normalised-expl))
            (/ (count term*) (count use-without-expl))
            0)

          ;; Ratio comparison with prefixed "to " removed
          (if (and (str/includes? use-without-to term*)
                   (not= use-without-to ""))
            (/ (count term*) (count use-without-to))
            0)))
      0)))

;; Decides which entries to include for English search results.
;; Really just an arbitrary value, but 0.33 seems to be an fair cutoff!
(def relevance-cutoff
  0.33)

(defn- above-cutoff?
  "Are any of the definitions above a the relevance cutoff for english-key?"
  [definitions english-key]
  (let [english-relevance-score* (partial english-relevance-score english-key)
        scores                   (map english-relevance-score* definitions)]
    (if (not (empty? scores))
      (> (apply max scores)
         relevance-cutoff))))

(defn add-english
  "Add an entry to the English dictionary from a CC-CEDICT listing.
  Keys (= single English words) are only added if they're above a certain
  relevance cutoff in order to limit the results list."
  [dict listing]
  (let [definitions (:definitions listing)
        ks          (->> (english-keys definitions)
                         (filter (partial above-cutoff? definitions)))
        v           (hash-set (:traditional listing) (:simplified listing))]
    (loop [dict* dict
           ks*   ks]
      (if (seq ks*)
        (recur (add dict* (first ks*) v) (rest ks*))
        dict*))))

;; Used on the frontend for sorting results.
;; Note that this - in addition to basic relevance - also considers frequency.
(defn english-relevance
  "Calculate the relevance of entry based on an English word as the search term.
  The relevance is a score from 0 to ~1, higher being more relevant.
  Relevance is able to exceed 1 slightly, as word frequency is also added to the
  score, allowing for more accurate sorting (it is a number from 0 to 1 that
  tends towards 0). This is what puts e.g. 句子 ahead of 语句 for 'sentence'."
  [term entry]
  (let [uses      (->> (vals (:uses entry))
                       (apply set/union))
        score     (partial english-relevance-score term)
        scores    (map score uses)
        max-score (apply max scores)
        freq      (get entry :frequency 0)]
    ;; Note: multiple 0.0 scores only count as a single zero!
    ;; This is done to not unfairly weigh down words with many meanings.
    (+ max-score freq)))

;;;; FREQUENCY DICTIONARY

(defn add-freq
  "Add word frequency (not char frequency) to a listing."
  [freq-dict listing]
  (let [trad-freq (get freq-dict (:traditional listing) 0)
        simp-freq (get freq-dict (:simplified listing) 0)
        frequency (max trad-freq simp-freq)]
    (if (> frequency 0)
      (assoc listing :frequency frequency)
      listing)))

;;; TODO: find proper thresholds for labels
(defn frequency-label
  "Get human-readable label for a given word frequency."
  [frequency]
  (cond
    (> frequency 0.01) :high
    (> 0.01 frequency 0.001) :medium
    :else :low))

;;;; CHARACTER ETYMOLOGY, DECOMPOSITION, ETC.

(defn ks->ns-keywords
  "Convert the top-level keys in a map to namespaced keywords."
  [ns m]
  (let [ns-k+v (fn [[k v]] [(keyword (str ns) (str k)) v])]
    (into {} (map ns-k+v m))))

(defn add-info*
  "Helper function for add-info."
  [script makemeahanzi listing]
  (if-let [info (get makemeahanzi (get listing script))]
    (let [decomposition (get info "decomposition")
          etymology     (if-let [raw (get info "etymology")]
                          (ks->ns-keywords 'sinostudy.dictionary.core raw)
                          nil)
          radical       (get info "radical")
          assoc*        (fn [coll k v]
                          (assoc-in coll [:info script k] v))]
      (cond-> listing
              decomposition (assoc* :decomposition decomposition)
              etymology (assoc* :etymology etymology)
              radical (assoc* :radical radical)))
    listing))

(defn add-info
  "Add info from makemeahanzi to a CC-CEDICT listing."
  [makemeahanzi listing]
  (->> listing
       (add-info* :traditional makemeahanzi)
       (add-info* :simplified makemeahanzi)))


;;;; CREATING DICTS AND LOOKING UP WORDS

(defn make-report
  "Create some some rudimentary statistics about the given dict."
  [dict]
  {:entry-count             (count (keys (:hanzi dict)))
   :english-count           (count (keys (:english dict)))
   :pinyin-count            (count (keys (:pinyin dict)))
   :pinyin+digits-count     (count (keys (:pinyin+digits dict)))
   :pinyin+diacritics-count (count (keys (:pinyin+diacritics dict)))})

;; TODO: also add listings only found in makemeahanzi (e.g. 忄)
(defn create-dict
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps.
  The listings convert into multiple dictionary entries based on look-up type.
  A freq-dict is used to add the word frequency to each entry if available."
  [listings freq-dict makemeahanzi]
  (let [listings*          (->> listings
                                (map detach-cls)
                                (map (partial add-freq freq-dict))
                                (map (partial add-info makemeahanzi)))
        add-pinyin-key     (partial add-pinyin :pinyin-key)
        add-digits-key     (partial add-pinyin :pinyin+digits-key)
        add-diacritics-key (partial add-pinyin :pinyin+diacritics-key)]
    (->> {:hanzi             (reduce add-hanzi {} listings*)
          :english           (reduce add-english {} listings*)
          :pinyin            (reduce add-pinyin-key {} listings*)
          :pinyin+digits     (reduce add-digits-key {} listings*)
          :pinyin+diacritics (reduce add-diacritics-key {} listings*)}
         (#(assoc %1 :report (make-report %1))))))

(defn look-up
  "Look up the specified term in each dictionary type.
  For Pinyin search results, both the raw search term and the pinyin-key version
  are looked up (results merged), e.g. 'ding zuo' also gets 'dingzuo'.
  Limit (optional) is a set of accepted result types."
  ([dict term limit]
   (let [term*       (pinyin-key term)                      ; unspaced
         look-up*    (fn [dict-type word] (-> dict (get dict-type) (get word)))
         limited     (fn [dict-type] (if limit (get limit dict-type) dict-type))
         get-entries (fn [words] (set (map #(look-up* :hanzi %) words)))
         hanzi       (look-up* (limited :hanzi) term)
         pinyin      (set/union (look-up* (limited :pinyin) term)
                                (look-up* (limited :pinyin) term*))
         digits      (set/union (look-up* (limited :pinyin+digits) term)
                                (look-up* (limited :pinyin+digits) term*))
         diacritics  (set/union (look-up* (limited :pinyin+diacritics) term)
                                (look-up* (limited :pinyin+diacritics) term*))
         english     (look-up* (limited :english) (str/lower-case term))
         result      (cond-> {:term term}
                             hanzi (assoc :hanzi #{hanzi})
                             pinyin (assoc :pinyin (get-entries pinyin))
                             digits (assoc :pinyin+digits (get-entries digits))
                             diacritics (assoc :pinyin+diacritics (get-entries diacritics))
                             english (assoc :english (get-entries english)))]
     (if (= result {:term term})
       nil
       result)))
  ([dict word]
   (look-up dict word nil)))


;;;; POST-PROCESSING DICTIONARY LOOK-UP RESULTS

(defn- safe-comparator
  "Create a comparator for  sorting that will not lose items by accident.
  When fn1 cannot establish an ordering between two elements, fn2 steps in.
  Based on example at: https://clojuredocs.org/clojure.core/sorted-set-by"
  [fn1 fn2]
  (fn [x y]
    (let [comparison (compare (fn1 x) (fn1 y))]
      (if (not= comparison 0)
        comparison
        (compare (fn2 x) (fn2 y))))))

(defn defs-containing-term
  "Only keep definitions that contain the given term."
  [term definitions]
  (let [term-re    (re-pattern (str "(?i)(^|[ (\"])" term "($|[ ,;.'!?)\"])"))
        with-term? (fn [definition]
                     (re-find term-re (remove-embedded definition)))]
    (filter with-term? definitions)))

(defn filter-defs
  "Remove definitions from entries if they do not contain the given term.
  Used to filter results by an English search term."
  [term entries]
  (let [relevant-defs (fn [[pinyin definitions]]
                        [pinyin (defs-containing-term term definitions)])
        non-empty     (comp seq second)]
    (for [entry entries]
      (assoc entry :uses (->> (:uses entry)
                              (map relevant-defs)
                              (filter non-empty)
                              (into {}))))))

(defn filter-uses
  "Remove uses from entries if the Pinyin does not match the given term.
  Used to filter results by a Pinyin search term.
  An optional normalisation function f can be supplied to convert the uses
  (normally in pinyin+digits format) to a Pinyin format matching the term."
  ([term entries f]
   (let [use-matches-term? (comp (fn [s] (= s (pinyin-key term)))
                                 pinyin-key
                                 (if f f identity)
                                 first)]
     (for [entry entries
           :let [uses (:uses entry)]]
       (assoc entry :uses (into {} (filter use-matches-term? uses))))))
  ([term entries]
   (filter-uses term entries nil)))

(defn reduce-result
  "Reduce the content of a dictionary look-up result.
  This removes irrelevant data from the result relative to the search term,
  e.g. removes definitions that do not match the search term."
  [result]
  (let [term       (:term result)
        pinyin     (:pinyin result)
        digits     (:pinyin+digits result)
        diacritics (:pinyin+diacritics result)
        hanzi      (:hanzi result)
        english    (:english result)]
    ; Reduce to single hanzi entry when applicable.
    ; Note: `hanzi` can only be a set of length 1 or nil!
    (if hanzi
      (first hanzi)
      (cond-> result

              pinyin
              (assoc :pinyin
                     (filter-uses term pinyin p/no-digits))

              digits
              (assoc :pinyin+digits
                     (filter-uses term digits))

              diacritics
              (assoc :pinyin+diacritics
                     (filter-uses term diacritics p/digits->diacritics))))))

;; TODO: disabled for now, re-enable when more intelligent (issue #37)
;english
;(assoc :english
;       (filter-defs term english)))))

(defn sort-result
  "Sort the content of a dictionary look-up result.
  This sorts the result relative to the search term,
  e.g English word results are sorted according to relevance."
  [result]
  (let [relevance  (memoize (partial english-relevance (:term result)))
        relevance* (comp - (safe-comparator relevance :term))
        sorted     (fn [f coll] (apply sorted-set-by f coll))
        pinyin     (:pinyin result)
        digits     (:pinyin+digits result)
        diacritics (:pinyin+diacritics result)
        english    (:english result)]
    (cond-> result
            ;pinyin (assoc :pinyin (sorted > pinyin))
            ;digits (assoc :pinyin+digits (sorted > digits))
            ;diacritics (assoc :pinyin+diacritics (sorted > diacritics))
            ;; TODO: sort Pinyin properly too
            pinyin (assoc :pinyin pinyin)
            digits (assoc :pinyin+digits digits)
            diacritics (assoc :pinyin+diacritics diacritics)
            english (assoc :english (sorted relevance* english)))))
