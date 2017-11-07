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
  ::query
  (fn [db [_ input]]
    (let [existing-queries (:queries db)
          id (count existing-queries)
          new-query {:content input :id id}]
      (assoc db :queries (conj existing-queries new-query)))))
