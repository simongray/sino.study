(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.db :refer [static-db]]))

(rf/reg-sub
  ::button-label
  (fn [db]
    (:button-label db)))

(rf/reg-sub
  ::input
  (fn [db]
    (:input db)))

(rf/reg-sub
  ::evaluation
  (fn [db]
    (first (:evaluations db))))

(rf/reg-sub
  ::input-css-class
  (fn [_]
    [(rf/subscribe [::input])
     (rf/subscribe [::evaluation])])
  (fn [[input evaluation]]
    (when (and evaluation
               (empty? (:actions evaluation))
               (not= "" (:query evaluation)))
      "no-actions")))

(rf/reg-sub
  ::hint
  (fn [db]
    (first (:hints db))))

(rf/reg-sub
  ::hint-content
  (fn [_]
    [(rf/subscribe [::hint])])
  (fn [[hint]]
    (let [hint-type (if hint (:type hint) :default)]
      (get (:hint-content static-db) hint-type))))

(rf/reg-sub
  ::hint-key
  (fn [_]
    [(rf/subscribe [::hint])])
  (fn [[hint]]
    (:type hint)))

(rf/reg-sub
  ::queries
  (fn [db]
    (:queries db)))
