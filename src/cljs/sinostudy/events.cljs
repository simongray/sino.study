(ns sinostudy.events
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [sinostudy.db :as db]
            [ajax.core :as ajax]))

(defn evaluate-query
  "Evaluates a piece of input text and returns a vector of possible actions."
  [s]
  (cond
    (= s "") []
    (= s "bad") []
    :else [:analyse-text])) ; TODO

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

(defn add-evaluation
  "Returns a list of evaluations with the new evaluation prepended."
  [evaluations query actions timestamp]
  (conj evaluations {:id        (count evaluations)
                     :query     query
                     :actions   actions
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
  ::display-hint
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ hint-type]]
    (let [db (:db cofx)
          hints (:hints db)
          now (:now cofx)]
      {:db (assoc db :hints (add-hint hints hint-type now))})))

;; dispatched by both ::evaluate-input and ::on-study-button-press
(rf/reg-event-fx
  ::save-evaluation
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ query actions]]
    (let [db (:db cofx)
          evaluations (:evaluations db)
          now (:now cofx)]
      {:db (assoc db :evaluations (add-evaluation evaluations
                                                  query
                                                  actions
                                                  now))})))

;; dispatched by ::on-input-change
;; only evaluates the latest input (no change while still writing)
;; this improves performance when coupled with delayed dispatching
(rf/reg-event-fx
  ::evaluate-input
  (fn [cofx [_ input]]
    (let [db (:db cofx)]
      (when (and (= input (:input db)))
        (let [actions (evaluate-query input)
              action-count (count actions)
              new-hint (cond
                         (= "" input) :default
                         (= 0 action-count) :no-actions
                         (= 1 action-count) (first actions)
                         :else :choose-action)]
          {:dispatch-n (list [::save-evaluation input actions]
                             [::display-hint new-hint])})))))

;; dispatched every time the input field changes
;; for performance reasons, non-blank queries are evaluated with a short lag
;; while blank queries are dispatched immediately for evaluation
;; immediate evaluation for blank input will override queued queries
;; this prevents any hint-changing misfires after clearing the input
;; otherwise, a queued query could modify the UI shortly after
(rf/reg-event-fx
  ::on-input-change
  (fn [cofx [_ input]]
    (let [db (:db cofx)
          blank-input? (string/blank? input)
          evaluation-lag (if blank-input? 0 500)
          trimmed-input (string/trim input)]
      {:db (assoc db :input input)
       :dispatch (when blank-input? [::display-hint :default])
       :dispatch-later [{:ms evaluation-lag
                         :dispatch [::evaluate-input trimmed-input]}]})))

;; dispatched upon a successful retrieval of a query result
(rf/reg-event-fx
  ::on-query-success
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db (:db cofx)
          queries (:queries db)
          now (:now cofx)]
      {:db (assoc db :queries (add-query queries :success result now))
       :dispatch [::display-hint :default]})))

;; dispatched upon an unsuccessful retrieval of a query result
(rf/reg-event-fx
  ::on-query-failure
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db (:db cofx)
          queries (:queries db)
          now (:now cofx)]
      {:db (assoc db :queries (add-query queries :failure result now))
       :dispatch [::display-hint :query-failure]})))


;; dispatched by ::on-study-button-press
(rf/reg-event-fx
   ::send-query
   (fn [cofx [_ query]]
     {:dispatch [::display-hint :examining]
      :http-xhrio {:uri "http://localhost:3000/query"
                   :method :get
                   :timeout 5000
                   :response-format (ajax/text-response-format)
                   :on-success [::on-query-success]
                   :on-failure [::on-query-failure]}}))

(rf/reg-event-fx
  ::choose-action
  (fn [_ _]
    {})) ;; TODO

;; dispatched by clicking the study button
;; forces an evaluation for the latest input if it hasn't been evaluated yet
(rf/reg-event-fx
  ::on-study-button-press
  (fn [cofx [_ input]]
    (let [db (:db cofx)
          latest-evaluation (first (:evaluations db))
          trimmed-input (string/trim input)
          query-changed? (not= trimmed-input (:query latest-evaluation))
          actions (if query-changed?
                    (evaluate-query trimmed-input)
                    (:actions latest-evaluation))
          action-count (count actions)]
      {:dispatch-n [(when query-changed?
                      [::save-evaluation trimmed-input actions])
                    (cond
                      (= 0 action-count) [::display-hint :no-actions]
                      (= 1 action-count) [::send-query trimmed-input]
                      :else              [::choose-action actions])]})))
