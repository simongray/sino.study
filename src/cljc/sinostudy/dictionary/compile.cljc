(ns sinostudy.dictionary.compile
  (:require [clojure.set :as set]
            [sinostudy.dictionary.core :as d]
            [sinostudy.dictionary.entry :as entry]))

(defn update-dict
  "Update the dictionary m at the specified key k with the entry v.
  The entry is either inserted as is or merged with the current entry."
  [m k entry]
  (if-let [current (get m k)]
    (let [scripts (set/union (d/scripts current) (d/scripts entry))
          cls     (set/union (d/cls current) (d/cls entry))
          uses    (merge-with set/union (d/uses current) (d/uses entry))
          vars    (merge-with set/union (d/vars current) (d/vars entry))]
      (assoc m k (cond-> current
                         scripts (assoc d/scripts scripts)
                         cls (assoc d/cls cls)
                         uses (assoc d/uses uses)
                         vars (assoc d/vars vars))))
    (assoc m k entry)))

(defn make-entry
  "Make a dictionary entry based on a script and a CC-CEDICT entry map."
  [script m]
  (let [script-diff? (not= (d/trad m) (d/simp m))
        other        (fn [script] (if (= script d/trad) d/simp d/trad))
        make-vars    (fn [script] {(other script) #{(get m (other script))}})
        cls          (d/cls m)
        base-entry   {d/scripts #{script}
                      d/uses    {(d/pinyin m) (d/defs m)}}]
    (cond-> base-entry
            script-diff? (assoc d/vars (make-vars script))
            cls (assoc d/cls cls))))

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
