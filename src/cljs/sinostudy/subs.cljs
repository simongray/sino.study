(ns sinostudy.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::button-label
  (fn [db]
    (:button-label db)))

(rf/reg-sub
  ::input
  (fn [db]
    (:input db)))

(rf/reg-sub
  ::input-placeholder
  (fn [db]
    (:input-placeholder db)))

(rf/reg-sub
  ::evaluation
  (fn [db]
    (:evaluation db)))

(rf/reg-sub
  ::hint
  (fn [db]
    (first (:hints db))))

(rf/reg-sub
  ::hint-contents
  (fn [db]
    (:hint-contents db)))

(rf/reg-sub
  ::hint-content
  (fn [_]
    [(rf/subscribe [::hint])
     (rf/subscribe [::hint-contents])])
  (fn [[hint hint-contents]]
    (get hint-contents (if hint
                         (:type hint)
                         :default))))

(rf/reg-sub
  ::hint-key
  (fn [_]
    [(rf/subscribe [::hint])])
  (fn [[hint]]
    (if (= (:type hint) :evaluating)
      "evaluation"
      (:id hint))))

(rf/reg-sub
  ::queries
  (fn [db]
    (:queries db)))
