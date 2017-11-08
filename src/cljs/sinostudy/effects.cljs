(ns sinostudy.effects
  (:require [re-frame.core :as rf]))

;; this namespace contains both (side-)effects -AND- (environment) co-effects

(rf/reg-cofx
  ::now
  (fn [cofx _]
    (assoc cofx :now (js/Date.))))

(rf/reg-cofx
  ::local-storage
  (fn [cofx local-storage-key]
    (assoc cofx :local-storage (js->clj (.getItem js/localStorage local-storage-key)))))
