(ns sinostudy.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::button-label
  (fn [db]
    (:button-label db)))

(re-frame/reg-sub
  ::input
  (fn [db]
    (:input db)))