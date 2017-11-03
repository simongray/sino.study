(ns sinostudy.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::study-button-label
 (fn [db]
   (:study-button-label db)))
