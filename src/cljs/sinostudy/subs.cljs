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