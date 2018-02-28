(ns sinostudy.dictionary.compile
  (:require [clojure.set :as set]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.entry :as entry]))

(defn if-assoc
  "Assoc value v at key k in map m, but only if v is non-nil."
  [m k v]
  (if v
    (assoc m k v)
    m))

(defn update-dict
  "Update the dictionary m at the specified key k with the entry v.
  The entry is either inserted as is or merged with the current entry."
  [m k entry]
  (if-let [current (get m k)]
    (let [scripts  (set/union (d/scripts current) (d/scripts entry))
          cls      (set/union (d/cls current) (d/cls entry))
          uses     (merge-with set/union (d/uses current) (d/uses entry))
          vars     (merge-with set/union (d/vars current) (d/vars entry))]
      (assoc m k (-> current
                     (assoc d/scripts scripts)
                     (assoc d/uses uses)
                     (if-assoc d/vars vars)
                     (if-assoc d/cls cls))))
    (assoc m k entry)))

(defn make-entry
  "Make a dictionary entry based on a script and a CC-CEDICT entry map."
  [script m]
  (-> {d/scripts #{script}
       d/uses    {(d/pinyin m) (d/defs m)}}
      (#(if (not= (d/trad m) (d/simp m))
          (let [other (if (= script d/trad) d/simp d/trad)]
            (assoc % d/vars {other #{(get m other)}}))
          %))
      (if-assoc d/cls (d/cls m))))

(defn add-hanzi
  "Create a hanzi entry in the dictionary from a basic CC-CEDICT entry m."
  [dict m]
  (-> dict
      (update-dict (get m d/trad) (make-entry d/trad m))
      (update-dict (get m d/simp) (make-entry d/simp m))))

(defn make-hanzi-dict
  [entries]
  (reduce add-hanzi {} entries))

;; TODO: update
(defn create-dicts
  "Load the contents of a CC-CEDICT dictionary file into Clojure maps."
  [entries key-types]
  (->> entries
       (map entry/detach-cls)
       (make-hanzi-dict)))
