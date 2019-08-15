(ns sinostudy.events.core
  "For miscellaneous events that do not have their own more specific namespace."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [re-frame.core :as rf]
            [ajax.core :as ajax]
            [cognitect.transit :as transit]
            [sinostudy.db :as db]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]
            [sinostudy.dictionary.core :as d]
            [sinostudy.navigation.pages :as pages]
            [sinostudy.events.scrolling :as scrolling]
            [sinostudy.events.actions :as actions]
            [sinostudy.cofx :as cofx]
            [sinostudy.fx :as fx]))

;; all responses from the Compojure backend are Transit-encoded
(def transit-reader
  (transit/reader :json))

(defn available-actions
  "Evaluate a query string to get a vector of possible actions."
  [query]
  (let [query*              (p/with-umlaut query)
        pinyin-block?       (or (pe/pinyin-block? query*)
                                (pe/pinyin-block+digits? query*)
                                (pe/pinyin-block+diacritics? query*))
        diacritics->digits? (and (pe/pinyin+diacritics+punct? query*)
                                 (not (pe/pinyin+punct? query*)))
        digits->diacritics? (and (pe/pinyin+digits+punct? query*)
                                 (not (pe/pinyin+punct? query*)))]
    (cond
      (pe/hanzi-block? query*)
      [[::look-up query*]]

      (re-find #"^\w" query*)
      (cond-> [[::look-up query*]]

              (and pinyin-block?
                   (not= query query*))
              (conj [::look-up (d/pinyin-key query*)])

              digits->diacritics?
              (conj [::digits->diacritics query*])

              diacritics->digits?
              (conj [::diacritics->digits query*])))))

(defn- cache-dict-entries
  "Save the individual entries of a dictionary search result in the db.
  Note: this is a separate step from saving the search result itself!"
  [db content]
  (let [path      [:pages ::pages/terms]
        entry-ks  #{::d/english
                    ::d/hanzi
                    ::d/pinyin
                    ::d/pinyin+diacritics
                    ::d/pinyin+digits}
        entries   (->> (select-keys content entry-ks)
                       (vals)
                       (apply set/union))
        add-entry (fn [db entry]
                    (assoc-in db (conj path (::d/term entry)) entry))]
    (reduce add-entry db entries)))

(defn mk-input
  "What the input field should display based on a given page."
  [[category id :as page]]
  (cond
    (= ::pages/terms category) (when (not (pe/hanzi-block? id)) id)))

;;;; MISCELLANEOUS

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/initial-db))

(rf/reg-event-db
  ::decompose-char
  (fn [db [_ decomposition]]
    (assoc db :decomposed decomposition)))

(rf/reg-event-db
  ::change-script
  (fn [db [_ script]]
    (assoc db :script script)))

(rf/reg-event-db
  ::set-result-filter
  (fn [db [_ term type]]
    (assoc-in db [:result-filters term] type)))

(rf/reg-event-fx
  ::blur-active-element
  [(rf/inject-cofx ::cofx/active-element)]
  (fn [{:keys [::cofx/active-element] :as cofx} _]
    {::fx/blur active-element}))


;;;; EVALUATION

(rf/reg-event-fx
  ::save-evaluation
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db ::cofx/now] :as cofx} [_ query actions]]
    {:db (update db :evaluations conj {:query     query
                                       :actions   actions
                                       :timestamp now})}))

;; Only evaluates the latest input (no change while still writing).
;; This improves performance when coupled with delayed dispatching
;; also doesn't evaluate the same query twice in a row!
(rf/reg-event-fx
  ::evaluate-input
  (fn [{:keys [db] :as cofx} [_ input]]
    (let [latest-evaluation (first (:evaluations db))
          latest-input?     (= input (:input db))
          query             (str/trim input)
          new-query?        (not= query (:query latest-evaluation))
          actions           (available-actions query)]
      (when (and latest-input? new-query?)
        {:dispatch-n [[::save-evaluation query actions]
                      (when (and actions
                                 (= ::look-up (-> actions first first)))
                        (first actions))]}))))

;; Dispatched every time the input field changes.
;; For performance reasons, non-blank queries are evaluated with a short lag
;; while blank queries are dispatched immediately for evaluation.
;; Immediate evaluation for blank input will override queued queries
;; this prevents any hint-changing misfires after clearing the input.
;; Otherwise, a queued query could modify the UI shortly after.
(rf/reg-event-fx
  ::on-input-change
  (fn [{:keys [db] :as cofx} [_ input]]
    (let [delay (get-in db [:config :evaluation :delay])
          fx    {:db (assoc db :input input)}]
      (if (str/blank? input)
        (assoc fx :dispatch [::evaluate-input input])
        (assoc fx :dispatch-later [{:dispatch [::evaluate-input input]
                                    :ms       delay}])))))


;;;; CHANGING LOCATION & LOADING PAGE CONTENT

;;; Force an evaluation for the latest input if it hasn't been evaluated yet.
(rf/reg-event-fx
  ::submit
  (fn [{:keys [db] :as cofx} [_ input]]
    (let [latest-eval (first (:evaluations db))
          query       (str/trim input)
          new-query?  (not= query (:query latest-eval))
          actions     (if new-query?
                        (available-actions query)
                        (:actions latest-eval))
          n           (count actions)]
      {:dispatch-n (cond-> []
                           new-query? (conj [::save-evaluation query actions])
                           (= n 1) (concat (conj actions [::blur-active-element]))
                           (> n 1) (conj [::actions/open-action-chooser]))})))

;; Pages are loaded on-demand from either the frontend db or (if N/A) by sending
;; a request to the backend. Currently, only dictionary pages are supported.
(rf/reg-event-fx
  ::load-page
  (fn [{:keys [db] :as cofx} [_ [category _ :as page]]]
    (let [{:keys [unknown-queries pages]} db]
      (when (= category ::pages/terms)
        (if (and (not (contains? unknown-queries page))
                 (not (get-in pages page)))
          {:dispatch [::request page]}
          {:dispatch [::update-location page]})))))

(rf/reg-event-fx
  ::enqueue
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db ::cofx/now] :as cofx} [_ page]]
    {:db (update db :queue conj (with-meta page {:ts now}))}))

(rf/reg-event-db
  ::dequeue
  (fn [db [_ page]]
    (update db :queue disj page)))

;; If a page doesn't exist in the frontend db, the backend will be contacted
;; through an Ajax request. While the request is underway, the requested page
;; is put in a queue. While requests are enqueued, they will not be retried.
;; Once a request for a page has been fulfilled or failed, the page will be
;; dequeued once again, allowing for new requests to be sent.
(rf/reg-event-fx
  ::request
  (fn [{:keys [db queue] :as cofx} [_ [category id :as page]]]
    (let [uri (str (:query-uri db) "/" (name category) "/" id)]
      (when (not (contains? queue page))
        {:dispatch   [::enqueue page]
         :http-xhrio {:method          :get
                      :timeout         5000
                      :response-format (ajax/text-response-format)
                      :on-success      [::on-request-success]
                      :on-failure      [::on-request-failure]
                      :uri             uri}}))))

(rf/reg-event-fx
  ::on-request-success
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db ::cofx/now] :as cofx} [_ result]]
    (let [content (transit/read transit-reader result)
          page    (:page content)]
      {:db         (update db :queries conj {:state     :success
                                             :content   content
                                             :timestamp now})
       :dispatch-n [[::dequeue page]
                    [::save-page content]]})))

(rf/reg-event-fx
  ::on-request-failure
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db ::cofx/now] :as cofx} [_ result]]
    {:db (update db :queries conj {:state     :failure
                                   :content   result
                                   :timestamp now})}))

;; Successful request to the backend lead to the retrieved page being saved in
;; the frontend db. In cases where a term does not have an associated page,
;; it is registered as unknown to prevent further retrieval attempts.
(rf/reg-event-fx
  ::save-page
  (fn [_ [_ {:keys [page result]}]]
    (let [[category id] page]
      {:dispatch-n [(cond
                      (nil? result) [::register-unknown-query id]
                      (= category ::pages/terms) [::save-term page result])
                    [::update-location page]]})))

(rf/reg-event-db
  ::register-unknown-query
  (fn [db [_ term]]
    (update db :unknown-queries conj term)))

;; Store result directly and then store individual entries.
;; TODO: reduce overwrites for hanzi result?
(rf/reg-event-db
  ::save-term
  (fn [db [_ [category id :as page] result]]
    (-> db
        ;; Cache the actual search result in the db.
        (assoc-in [:pages category id] (-> result
                                           (d/reduce-result)
                                           (d/sort-result)))

        ;; Cache incidental, referenced entries for faster page rendering times.
        (cache-dict-entries result))))

;; Dispatched either directly by ::load-page or indirectly through a successful
;; backend request. This ensures that the address bar is only updated when
;; content actually exists.
(rf/reg-event-fx
  ::update-location
  [(rf/inject-cofx ::cofx/pathname)]
  (fn [{:keys [db ::cofx/pathname] :as cofx} [_ [_ id :as page]]]
    (let [{:keys [input unknown-queries]} db]
      (when (and (= input id)
                 (not (contains? unknown-queries id))
                 (not (pages/equivalent? pathname page)))
        {::fx/navigate-to (pages/page->pathname page)}))))

(rf/reg-event-fx
  ::change-location
  [(rf/inject-cofx ::cofx/now)
   (rf/inject-cofx ::cofx/scroll-state)]
  (fn [{:keys [db ::cofx/now ::cofx/scroll-state] :as cofx} [_ new-page]]
    (let [{:keys [input history]} db
          current-page (first history)]
      {:db         (-> db
                       (update :history conj (with-meta new-page now))
                       (assoc :input (or input
                                         (mk-input new-page))))
       :dispatch-n [[::scrolling/save-scroll-state current-page scroll-state]
                    [::load-page (pages/shortened new-page)]]})))

(rf/reg-event-fx
  ::look-up
  (fn [_ [_ term]]
    {:dispatch [::load-page [::pages/terms term]]}))
