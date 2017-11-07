(ns sinostudy.events
  (:require [re-frame.core :as re-frame]
            [sinostudy.db :as db]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  ::input-change
  (fn [db [_ new-input]]
    (assoc db :input new-input)))

(re-frame/reg-event-db
  ::study
  (fn [db _]
    (do (println db) ; smelly! just for testing
        db)))
