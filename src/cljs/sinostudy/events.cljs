(ns sinostudy.events
  (:require [re-frame.core :as rf]
            [sinostudy.db :as db]))

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(rf/reg-event-db
  ::input-change
  (fn [db [_ new-input]]
    (assoc db :input new-input)))

(rf/reg-event-db
  ::study
  (fn [db _]
    (do (println db) ; smelly! just for testing
        db)))
