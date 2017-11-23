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
    (:hint db)))

(rf/reg-sub
  ::queries
  (fn [db]
    (:queries db)))
