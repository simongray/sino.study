(ns sinostudy.dictionary.wordnet
  (:import [net.sf.extjwnl.dictionary Dictionary]
           [net.sf.extjwnl.data Word IndexWord Synset POS]))

(def d (Dictionary/getDefaultResourceInstance))

(comment
  (->> (.getIndexWord d POS/NOUN "dog")
       (.getSenses)
       (mapcat #(.getWords %))
       (map #(.getLemma %))))
