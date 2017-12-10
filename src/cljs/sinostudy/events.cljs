(ns sinostudy.events
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [sinostudy.db :as db]
            [sinostudy.evaluation :refer [eval-query]]
            [sinostudy.pinyin.core :as pinyin]
            [ajax.core :as ajax]))

;;;; HELPER FUNCTIONS

(defn add-query
  "Returns a list of queries with the new query prepended."
  [queries state content timestamp]
  (conj queries {:id        (count queries)
                 :state     state
                 :content   content
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

(defn add-page
  "Returns a new page map with the specified page updated."
  [pages page-type key content-type content timestamp]
  (assoc-in pages [page-type key] {:content      content
                                   :content-type content-type
                                   :timestamp    timestamp}))

(defn new-page
  "Returns an updated page map with the current page set to the new page."
  [pages page-type key content-type content timestamp]
  (-> pages
      (assoc :history (conj (:history pages) [page-type key]))
      (add-page page-type key content-type content timestamp)))

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
    (let [db        (:db cofx)
          hints     (:hints db)
          now       (:now cofx)
          new-hints (add-hint hints hint-type now)]
      {:db (assoc db :hints new-hints)})))

;; dispatched by both ::evaluate-input and ::on-study-button-press
(rf/reg-event-fx
  ::save-evaluation
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ query actions]]
    (let [db              (:db cofx)
          evaluations     (:evaluations db)
          now             (:now cofx)
          new-evaluations (add-evaluation evaluations query actions now)]
      {:db (assoc db :evaluations new-evaluations)})))

;; dispatched by ::on-input-change
;; only evaluates the latest input (no change while still writing)
;; this improves performance when coupled with delayed dispatching
;; also doesn't evaluate the same query twice in a row!
(rf/reg-event-fx
  ::evaluate-input
  (fn [cofx [_ input]]
    (let [db                (:db cofx)
          latest-evaluation (first (:evaluations db))
          latest-input?     (= input (:input db))
          query             (string/trim input)
          new-query?        (not= query (:query latest-evaluation))]
      (when (and latest-input? new-query?)
        (let [actions  (eval-query query)
              new-hint (case (count actions)
                         0 :no-actions
                         1 (first actions)
                         (if (empty? query) :default :choose-action))]
          {:dispatch-n (list [::save-evaluation query actions]
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
          fx {:db (assoc db :input input)}]
      (if (string/blank? input)
        (assoc fx :dispatch-n [[::display-hint :default]
                               [::evaluate-input input]])
        (assoc fx :dispatch-later [{:dispatch [::evaluate-input input]
                                    :ms       1000}])))))

;; dispatched upon a successful retrieval of a query result
(rf/reg-event-fx
  ::on-query-success
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db      (:db cofx)
          queries (:queries db)
          now     (:now cofx)]
      {:db       (assoc db :queries (add-query queries :success result now))
       :dispatch [::display-hint :default]})))

;; dispatched upon an unsuccessful retrieval of a query result
(rf/reg-event-fx
  ::on-query-failure
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db      (:db cofx)
          queries (:queries db)
          now     (:now cofx)]
      {:db       (assoc db :queries (add-query queries :failure result now))
       :dispatch [::display-hint :query-failure]})))

(rf/reg-event-fx
  ::send-query
  (fn [cofx [_ query]]
    {:dispatch   [::display-hint :examining]
     :http-xhrio {:uri             "http://localhost:3000/query"
                  :method          :get
                  :timeout         5000
                  :response-format (ajax/text-response-format)
                  :on-success      [::on-query-success]
                  :on-failure      [::on-query-failure]}}))

(rf/reg-event-fx
  ::perform-action
  (fn [cofx [_ action]]
    (let [db    (:db cofx)
          input (:input db)]
      (case action
        :test {:dispatch [::test]}
        :clear {:dispatch [::initialize-db]}
        :digits->diacritics {:dispatch [::digits->diacritics input]}))))

(rf/reg-event-fx
  ::choose-action
  (fn [_ _]
    {}))                                                    ;; TODO

;; dispatched by clicking the study button (= pressing enter)
;; forces an evaluation for the latest input if it hasn't been evaluated yet
(rf/reg-event-fx
  ::on-submit
  (fn [cofx [_ input]]
    (let [db                (:db cofx)
          latest-evaluation (first (:evaluations db))
          query             (string/trim input)
          new-query?        (not= query (:query latest-evaluation))
          actions           (if new-query?
                              (eval-query query)
                              (:actions latest-evaluation))]
      {:dispatch-n [(case (count actions)
                      0 [::display-hint :no-actions]
                      1 [::perform-action (first actions)]
                      [::choose-action actions])
                    (when new-query? [::save-evaluation query actions])]})))


;;;; ACTIONS (= events triggered by submitting input)

;; dispatched by ::do-action
(rf/reg-event-fx
  ::test
  [(rf/inject-cofx ::now)]
  (fn [cofx _]
    (let [db           (:db cofx)
          pages        (:pages db)
          page-type    :tests
          key          "test"
          content-type :hiccup
          content      [:div [:h1 "Test"] [:p "This is a test page."]]
          now          (:now cofx)
          new-pages    (new-page pages page-type key content-type content now)]
      {:db       (-> db
                     (assoc :pages new-pages)
                     (assoc :input ""))
       :dispatch [::display-hint :default]})))

;; dispatched by ::do-action
(rf/reg-event-fx
  ::digits->diacritics
  (fn [cofx [_ input]]
    (let [db        (:db cofx)
          new-input (pinyin/digits->diacritics input)]
      {:db       (assoc db :input new-input)
       :dispatch [::display-hint :default]})))
