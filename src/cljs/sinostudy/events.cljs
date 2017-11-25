(ns sinostudy.events
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [sinostudy.db :as db]
            [ajax.core :as ajax]))

(defn evaluate
  [s]
  (if (string/blank? s)
    nil
    "evaluated"))

(defn add-query
  "Returns a list of queries with the new query prepended."
  [queries state content timestamp]
  (conj queries {:id (count queries)
                 :state state
                 :content content
                 :timestamp timestamp}))


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
    (let [hint-types (:hint-types db)]
      ;; only evaluates the latest input (no change while still evaluating)
      ;; improves performance when coupled with delayed dispatching
      (if (= (:input db) new-input)
        (-> db
            (assoc :evaluation (evaluate new-input))
            (assoc :hint (:default hint-types)))
        db))))

;; dispatched every time the input field changes
(rf/reg-event-fx
  ::input-change
  (fn [cofx [_ new-input]]
    (let [db (:db cofx)
          hint-types (:hint-types db)
          no-input (string/blank? new-input)
          new-hint (if no-input (:default hint-types) (:evaluating hint-types))
          evaluation-lag (if no-input 0 500)]
      {:db (-> db
               (assoc :input new-input)
               (assoc :evaluation nil)
               (assoc :hint new-hint)
               (assoc :input-placeholder "")) ; prevents respawn
       :dispatch-later [{:ms evaluation-lag
                         :dispatch [::evaluate-input new-input]}]})))

(rf/reg-event-fx
  ::query-success
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db (:db cofx)
          hint-types (:hint-types db)
          queries (:queries db)
          now (:now cofx)]
      {:db (-> db
               (assoc :queries (add-query queries :success result now))
               (assoc :hint (:default hint-types)))})))

(rf/reg-event-fx
  ::query-failure
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db (:db cofx)
          hint-types (:hint-types db)
          queries (:queries db)
          now (:now cofx)]
      {:db (-> db
               (assoc :queries (add-query queries :failure result now))
               (assoc :hint (:query-failure hint-types))
               (assoc :hint (:query-failure hint-types)))})))

;; send a query away for processing
(rf/reg-event-fx
   ::query
   [(rf/inject-cofx ::now)]
   (fn [cofx [_ input]]
     (let [db (:db cofx)
           hint-types (:hint-types db)
           now (:now cofx)
           ;; always force an evaluation if missing
           evaluation (if (:evaluation db) (:evaluation db) (evaluate input))]
       (if evaluation
         {:db (-> db
                  (assoc :input "")
                  (assoc :evaluation nil)
                  (assoc :hint (:examining hint-types)))
          :http-xhrio {:uri "http://localhost:3000/query"
                       :method :get
                       :timeout 5000
                       :response-format (ajax/text-response-format)
                       :on-success [::query-success]
                       :on-failure [::query-failure]}}
         db))))
