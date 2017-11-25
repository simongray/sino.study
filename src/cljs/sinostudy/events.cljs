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

(defn add-hint
  "Returns a list of hints with the new hint prepended."
  [hints hint-type timestamp]
  (conj hints {:id        (count hints)
               :type      hint-type
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

;; display hints below the input field
(rf/reg-event-fx
  ::hint
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ hint-type]]
    (let [db (:db cofx)
          hints (:hints db)
          now (:now cofx)]
      {:db (assoc db :hints (add-hint hints hint-type now))})))

;; evaluate input query
;; this event is dispatched by ::input-change
(rf/reg-event-fx
  ::evaluate-input
  (fn [cofx [_ new-input]]
    (let [db (:db cofx)]
      ;; only evaluates the latest input (no change while still evaluating)
      ;; improves performance when coupled with delayed dispatching
      (if (= (:input db) new-input)
        {:db (assoc db :evaluation (evaluate new-input))
         :dispatch [::hint :default]}
        {:db db}))))

;; dispatched every time the input field changes
(rf/reg-event-fx
  ::input-change
  (fn [cofx [_ new-input]]
    (let [db (:db cofx)
          no-input (string/blank? new-input)
          new-hint (if no-input :default :evaluating)
          evaluation-lag (if no-input 0 500)]
      {:db (-> db
               (assoc :input new-input)
               (assoc :evaluation nil)
               (assoc :input-placeholder "")) ; prevents respawn
       :dispatch [::hint new-hint]
       :dispatch-later [{:ms evaluation-lag
                         :dispatch [::evaluate-input new-input]}]})))

;; dispatched upon a successful retrieval of a query result
(rf/reg-event-fx
  ::query-success
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db (:db cofx)
          queries (:queries db)
          now (:now cofx)]
      {:db (assoc db :queries (add-query queries :success result now))
       :dispatch [::hint :default]})))

;; dispatched upon an unsuccessful retrieval of a query result
(rf/reg-event-fx
  ::query-failure
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db (:db cofx)
          queries (:queries db)
          now (:now cofx)]
      {:db (assoc db :queries (add-query queries :failure result now))
       :dispatch [::hint :query-failure]})))

;; send a query away for processing
(rf/reg-event-fx
   ::query
   [(rf/inject-cofx ::now)]
   (fn [cofx [_ input]]
     (let [db (:db cofx)
           now (:now cofx)
           ;; always force an evaluation if missing
           evaluation (if (:evaluation db) (:evaluation db) (evaluate input))]
       (if evaluation
         {:db (-> db
                  (assoc :input "")
                  (assoc :evaluation nil))
          :dispatch [::hint :examining]
          :http-xhrio {:uri "http://localhost:3000/query"
                       :method :get
                       :timeout 5000
                       :response-format (ajax/text-response-format)
                       :on-success [::query-success]
                       :on-failure [::query-failure]}}
         db))))
