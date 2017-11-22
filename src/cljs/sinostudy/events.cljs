(ns sinostudy.events
  (:require [re-frame.core :as rf]
            [sinostudy.db :as db]))

;; built-in effects: https://github.com/Day8/re-frame/blob/master/src/re_frame/fx.cljc


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

;; evaluate input (e.g. to display proper hints)
;; this event is dispatched by ::input-change with a small delay
;; only the latest dispatch changes state, earlier ones are ignored to improve performance
(rf/reg-event-db
  ::evaluate-input
  (fn [db [_ new-input]]
    (if (and
          (not (empty? new-input))
          (= (:input db) new-input)) ; only evaluate up-to-date input
        (-> db
            (assoc :evaluation 1) ; test data
            (assoc :input (str new-input " (evaluated)"))) ; test output
        db)))

;; dispatched every time the input field changes
(rf/reg-event-fx
  ::input-change
  (fn [cofx [_ new-input]]
    (let [db (:db cofx)
          now (:now cofx)]
      {:db (-> db
               (assoc :input new-input)
               (assoc :evaluation nil))
       :dispatch-later [{:ms 1000
                         :dispatch [::evaluate-input new-input]}]})))

;; send a query away for processing
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
