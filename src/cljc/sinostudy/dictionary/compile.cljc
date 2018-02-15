(ns sinostudy.dictionary.compile
  (:require [clojure.set :as set]
            [sinostudy.dictionary.entry :as entry]
            [sinostudy.dictionary.embed :as embed]
            [sinostudy.dictionary.core :as d]))


;;;; ENTRY MANIPULATION

(defn detach-cls
  "Move the classifiers of an entry from :definitions to :classifiers."
  [entry]
  (if (entry/has-cls? entry)
    (let [defs    (d/defs entry)
          cl-defs (filter entry/cl-def? defs)
          get-cls (comp (partial map embed/ref->m) (partial re-seq embed/ref))
          cls     (set (flatten (map get-cls cl-defs)))]
      (if cls
        (-> entry
            (assoc d/defs (set/difference defs cl-defs))
            (assoc d/cls cls))
        entry))
    entry))


;;;; DICTIONARY COMPILATION

(defn add-entry
  "Add (or extend) an entry in the dictionary map."
  [key-type dict entry]
  (let [key (get entry key-type)]
    (if-let [entries (get dict key)]
      (assoc dict key (conj entries entry))
      (assoc dict key #{entry}))))

(defn compile-dict
  "Create a dictionary map from the entries with keys determined by key-type,
  this being the field in the entry that must serve as key (e.g. :traditional)."
  [key-type entries]
  (reduce (partial add-entry key-type) {} entries))

(defn compile-dicts
  "Create a map of dictionary maps with different look-up key-types."
  [key-types entries]
  ;; TODO: remove println, enable some sort of logging instead
  (println "compiling" (count key-types) "dicts from" (count entries) "entries")
  (let [make-dict (fn [key-type] [key-type (compile-dict key-type entries)])]
    (into {} (map make-dict key-types))))

(defn merge-entry
  "Merge entry (e.g. name entry) into matching existing entries in dict.
  This both merges the definition into other entries and removes the old entry.
  In case there is only one entry, nothing happens.
  Returns dict with the entry merged in."
  [key-type dict entry]
  (let [key     (get entry key-type)
        entries (get dict key)]
    (if (= 1 (count entries))
      dict
      (loop [matches* (filter (partial entry/matches entry) entries)
             entries* entries]
        (let [match (first matches*)]
          (cond
            (nil? match) (assoc dict key entries*)
            (= match entry) (recur (rest matches*) (disj entries* match))
            :else (let [match-def (d/defs match)
                        entry-def (d/defs entry)
                        new-def   (set/union match-def entry-def)
                        new-entry (assoc match d/defs new-def)]
                    (recur (rest matches*) (-> entries*
                                               (disj match)
                                               (conj new-entry))))))))))

(defn merge-entries
  "Merge definitions of entries into matching entries in dict.
  Returns the modified dict."
  [key-type dict entries]
  (loop [dict*    dict
         entries* entries]
    (if-let [entry (first entries*)]
      (recur (merge-entry key-type dict* entry) (rest entries*))
      dict*)))

(defn mod-dicts
  "Merge the entries of merges into each dictionary map."
  [merges dicts]
  (println "merging" (count merges) "entries into" (count dicts) "dicts")
  (loop [keys   (keys dicts)
         dicts* dicts]
    (if-let [key (first keys)]
      (recur
        (rest keys)
        (assoc dicts* key (merge-entries key (get dicts* key) merges)))
      dicts*)))

(defn load-dicts
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [entries key-types]
  (let [name-entries (filter entry/name? entries)]
    (->> entries
         (map detach-cls)
         (compile-dicts key-types)
         (mod-dicts name-entries))))