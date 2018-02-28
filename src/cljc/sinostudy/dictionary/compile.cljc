(ns sinostudy.dictionary.compile
  (:require [clojure.set :as set]
            [sinostudy.dictionary.entry :as entry]))

(defn set-map-union
  "Merge two maps whose values are sets."
  [m1 m2]
  (let [merge (fn [m [k v]]
                (assoc m k (set/union (get m k) v)))]
    (reduce merge m1 m2)))

(defn update-dict
  "Update the dictionary m at the specified key k with the entry v.
  The entry is either inserted as is or merged with the current entry."
  [m k entry]
  (if-let [current (get m k)]
    (let [scripts     (set/union (:scripts current) (:scripts entry))
          usages      (set-map-union (:usages current) (:usages entry))
          variations  (set-map-union (:variations current) (:variations entry))
          classifiers (set/union (:classifiers current) (:classifiers entry))
          updated     (-> current
                          (assoc :scripts scripts)
                          (assoc :usages usages)
                          (#(if variations
                              (assoc % :variations variations)
                              %))
                          (#(if classifiers
                              (assoc % :classifiers classifiers)
                              %)))]
      (assoc m k updated))
    (assoc m k entry)))

(defn make-entry
  "Make a dictionary entry based on a script and a CC-CEDICT entry map."
  [script m]
  (-> {:scripts #{script}
       :usages  {(:pinyin m) (:definitions m)}}
      (#(if (not= (:traditional m) (:simplified m))
          (let [other (if (= script :traditional) :simplified :traditional)]
            (assoc % :variations {other #{(get m other)}}))
          %))
      (#(if-let [classifiers (:classifiers m)]
          (assoc % :classifiers classifiers)
          %))))

(defn add-hanzi
  "Create a hanzi entry in the dictionary from a basic CC-CEDICT entry m."
  [dict m]
  (-> dict
      (update-dict (get m :traditional) (make-entry :traditional m))
      (update-dict (get m :simplified) (make-entry :simplified m))))

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
