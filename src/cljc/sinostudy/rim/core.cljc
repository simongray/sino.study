(ns sinostudy.rim.core)


(defn match?
  "Test every predicate function in preds on objects x and y.
  Returns true (= x and y match) if all function calls return true."
  [x y & preds]
  (every? (fn [pred] (pred x y)) preds))

(defn all-matches
  "Get all entries in xs that match x based on predicate functions in preds."
  [x xs & preds]
  (filter (fn [y] (apply match? x y preds)) xs))
