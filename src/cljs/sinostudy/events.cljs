(ns sinostudy.events
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [accountant.core :as accountant]
            [sinostudy.db :as db]
            [sinostudy.queries :as q]
            [sinostudy.pinyin.core :as pinyin]
            [sinostudy.dictionary.core :as dict]
            [ajax.core :as ajax]
            [cognitect.transit :as transit]))

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
  "Returns a list of query evaluations with the new evaluation prepended."
  [evaluations query actions timestamp]
  (conj evaluations {:id        (count evaluations)
                     :query     query
                     :actions   actions
                     :timestamp timestamp}))

;; all responses from the Compojure backend are Transit-encoded
(def transit-reader
  (transit/reader :json))

(defn preprocess-content
  "Process content coming from the web service before storing locally,
  e.g. presort word lists and the like."
  [page-type content]
  (case page-type
    :word (dict/prepare-entries content)
    content))


;;;; CO-EFFECTS

(rf/reg-cofx
  ::now
  (fn [cofx _]
    (assoc cofx :now (js/Date.))))

(rf/reg-cofx
  ::local-storage
  (fn [cofx key]
    (assoc cofx :local-storage (js->clj (.getItem js/localStorage key)))))


;;;; EFFECTS (= side effects)

;; Dispatched by actions that need to change the page (and browser history).
(rf/reg-fx
  :navigate-to
  (fn [path]
    (accountant/navigate! path)))


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
        (let [actions  (q/eval-query query)
              ;; hints must match the name of the action!
              new-hint (case (count actions)
                         0 (if (empty? query) :default :no-actions)
                         1 (first actions)
                         :choose-action)]
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

;; dispatched by ::on-query-success
(rf/reg-event-db
  ::save-page
  (fn [db [_ {:keys [page result]}]]
    (let [path (into [:pages] page)
          page-type (first page)
          content (preprocess-content page-type result)]
      (assoc-in db path content))))

;; dispatched upon a successful retrieval of a query result
(rf/reg-event-fx
  ::on-query-success
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db      (:db cofx)
          queries (:queries db)
          content (transit/read transit-reader result)
          now     (:now cofx)]
      {:db       (assoc db :queries (add-query queries :success content now))
       :dispatch [::save-page content]})))

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

;; dispatched by ::load-content
(rf/reg-event-fx
  ::send-query
  (fn [_ [_ query]]
    (let [default-request {:method          :get
                           :timeout         5000
                           :response-format (ajax/text-response-format)
                           :on-success      [::on-query-success]
                           :on-failure      [::on-query-failure]}
          base-uri        "http://localhost:3000/query/"
          query-type      (name (first query))
          query-string    (second query)
          uri             (str base-uri query-type "/" query-string)
          request         (assoc default-request :uri uri)]
      {:http-xhrio request})))

;; dispatched by ::on-submit
(rf/reg-event-fx
  ::perform-action
  (fn [cofx [_ action]]
    (let [db    (:db cofx)
          input (:input db)]
      {:dispatch (case action
                   :test [::test]
                   :clear [::initialize-db]
                   :look-up-word [::look-up-word input]
                   :digits->diacritics [::digits->diacritics input])})))

;; dispatched by ::on-submit
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
                              (q/eval-query query)
                              (:actions latest-evaluation))]
      {:dispatch-n [(case (count actions)
                      0 [::display-hint :no-actions]
                      1 [::perform-action (first actions)]
                      [::choose-action actions])
                    (when new-query? [::save-evaluation query actions])]})))

;; dispatched by ::change-page
(rf/reg-event-fx
  ::load-content
  (fn [cofx [_ page]]
    (let [db        (:db cofx)
          pages     (:pages db)
          page-type (first page)]
      (case page-type
        :static {}
        :word (let [word-page (subvec page 0 2)]            ; remove numbering
                (if (not (get-in pages word-page))
                  {:dispatch [::send-query word-page]}
                  {}))))))

;; Dispatched by clicking links only!
;; It's never dispatched directly, as we want to leave a browser history trail.
;; Link-clicking is facilitated by frontend routing (secretary + Accountant).
(rf/reg-event-fx
  ::change-page
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ page]]
    (let [db        (:db cofx)
          history   (:history db)
          timestamp (:now cofx)]
      {:db       (assoc db :history (conj history [page timestamp]))
       :dispatch [::load-content page]})))

;; Dispatched by on-click handlers in various places.
(rf/reg-event-db
  ::change-script
  (fn [db [_ script]]
    (assoc db :script script)))

;;;; ACTIONS (= events triggered by submitting input)

(rf/reg-event-fx
  ::test
  [(rf/inject-cofx ::now)]
  (fn [_ _]
    {:dispatch [::send-query [:word "你好"]]}))

(rf/reg-event-db
  ::clear-input
  (fn [db _]
    (assoc db :input "")))

(rf/reg-event-fx
  ::look-up-word
  (fn [cofx [_ word]]
    (let [db (:db cofx)]
      {:db          (assoc db :input "")
       :navigate-to (str "/word/" word)
       :dispatch    [::display-hint :default]})))

(rf/reg-event-fx
  ::digits->diacritics
  (fn [cofx [_ input]]
    (let [db        (:db cofx)
          new-input (pinyin/digits->diacritics input)]
      {:db       (assoc db :input new-input)
       :dispatch [::display-hint :default]})))
