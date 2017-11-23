(ns sinostudy.events
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [sinostudy.db :as db]))

(defn evaluate
  [s]
  (if (string/blank? s)
    nil
    "evaluated"))

;;;; CO-EFFECTS

(rf/reg-cofx
  ::now
  (fn [cofx _]
    (assoc cofx :now (js/Date.))))

(rf/reg-cofx
  ::local-storage
  (fn [cofx key]
    (assoc cofx :local-storage (js->clj (.getItem js/localStorage key)))))


;;;; EVENTS

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

;; evaluate input (e.g. to display proper hints)
;; this event is dispatched by ::input-change
(rf/reg-event-db
  ::evaluate-input
  (fn [db [_ new-input]]
    (let [hints (:hints db)]
      ;; only evaluates the latest input (no change while still evaluating)
      ;; improves performance when coupled with delayed dispatching
      (if (= (:input db) new-input)
        (-> db
            (assoc :evaluation (evaluate new-input))
            (assoc :hint (:default hints)))
        db))))

;; dispatched every time the input field changes
(rf/reg-event-fx
  ::input-change
  (fn [cofx [_ new-input]]
    (let [db (:db cofx)
          hints (:hints db)
          no-input (string/blank? new-input)]
      {:db (-> db
               (assoc :input new-input)
               (assoc :evaluation nil)
               (assoc :hint (if no-input (:default hints) (:evaluating hints)))
               (assoc :input-placeholder "")) ; prevents respawn
       :dispatch-later [{:ms (if no-input 0 500)
                         :dispatch [::evaluate-input new-input]}]})))

;; send a query away for processing
(rf/reg-event-fx
   ::query
   [(rf/inject-cofx ::now)]
   (fn [cofx [_ input]]
     (let [db (:db cofx)
           hints (:hints db)
           now (:now cofx)
           existing-queries (:queries db)
           id (count existing-queries)
           new-query {:content input :id id :timestamp now}
           ;; always force an evaluation if missing
           evaluation (if (:evaluation db) (:evaluation db) (evaluate input))]
       (if evaluation
         {:db (-> db
                  (assoc :queries (conj existing-queries new-query))
                  (assoc :input "")
                  (assoc :evaluation nil)
                  (assoc :hint (:examining hints)))}
         db))))
