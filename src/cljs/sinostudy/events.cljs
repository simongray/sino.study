(ns sinostudy.events
  (:require [re-frame.core :as rf]
            [sinostudy.db :as db]))

;;;; CO-EFFECTS

(rf/reg-cofx
  ::now
  (fn [cofx _]
    (assoc cofx :now (js/Date.))))

(rf/reg-cofx
  ::local-storage
  (fn [cofx local-storage-key]
    (assoc cofx :local-storage (js->clj (.getItem js/localStorage local-storage-key)))))


;;;; EVENTS

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
      (-> db
          (assoc :queries (conj existing-queries new-query))
          (assoc :input "")))))

(rf/reg-event-fx
   ::query
   [(rf/inject-cofx ::now)]
   (fn [cofx [_ input]]
     (let [db (:db cofx)
           now (:now cofx)
           existing-queries (:queries db)
           id (count existing-queries)
           new-query {:content input :id id :timestamp now}]
       {:db (-> db
                (assoc :queries (conj existing-queries new-query))
                (assoc :input ""))})))
