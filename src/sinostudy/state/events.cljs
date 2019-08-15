(ns sinostudy.state.events
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [re-frame.core :as rf]
            [ajax.core :as ajax]
            [cognitect.transit :as transit]
            [sinostudy.state.db :as db]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]
            [sinostudy.dictionary.core :as d]
            [sinostudy.navigation.pages :as pages]
            [sinostudy.state.coeffects :as cofx]
            [sinostudy.state.effects :as fx]))

;; all responses from the Compojure backend are Transit-encoded
(def transit-reader
  (transit/reader :json))

(defn- save-dict-entries
  "Save the individual entries of a dictionary search result into the db.
  Note: this is a separate step from saving the search result itself!"
  [db content]
  (if (not (::d/unknown content))
    (let [path    [:pages ::pages/terms]
          entries (->> content
                       (filter #(-> % first (not= ::d/term)))
                       (map second)
                       (apply set/union))]
      (loop [db*      db
             entries* entries]
        (if (empty? entries*)
          db*
          (let [entry (first entries*)
                path* (conj path (::d/term entry))]
            (recur (assoc-in db* path* entry) (rest entries*))))))
    db))

(defn mk-input
  "What the input field should display based on a given page."
  [[category id]]
  (cond
    (= ::pages/terms category) (when (not (pe/hanzi-block? id)) id)))

;;;; QUERY EVALUATION

(defn eval-query
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
  (fn [cofx _]
    {::fx/blur (::cofx/active-element cofx)}))

;;;; SCROLLING

(rf/reg-event-fx
  ::save-scroll-states
  (fn [{:keys [db]} [_ page scroll-states]]
    (when page
      {:db (if (empty? scroll-states)
             (update db :scroll-states dissoc page)
             (assoc-in db [:scroll-states page] scroll-states))})))

(rf/reg-event-fx
  ::reset-scroll-state
  (fn [_ [_ page]]
    {:dispatch [::save-scroll-states page nil]}))

(rf/reg-event-fx
  ::load-scroll-state
  (fn [cofx [_ page]]
    (let [scroll-states (get-in cofx [:db :scroll-states page])]
      {::fx/set-scroll-states scroll-states})))


;;;; EVALUATION

(rf/reg-event-fx
  ::save-evaluation
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db] :as cofx} [_ query actions]]
    (let [now (::cofx/now cofx)]
      {:db (update db :evaluations conj {:query     query
                                         :actions   actions
                                         :timestamp now})})))

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
          actions           (eval-query query)]
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
                        (eval-query query)
                        (:actions latest-eval))
          n           (count actions)]
      {:dispatch-n (cond-> []
                           new-query? (conj [::save-evaluation query actions])
                           (= n 1) (concat (conj actions [::blur-active-element]))
                           (> n 1) (conj [::open-action-chooser]))})))

;; Pages are loaded on-demand from either the frontend db or (if N/A) by sending
;; a request to the backend. Currently, only dictionary pages are supported.
(rf/reg-event-fx
  ::load-page
  (fn [{:keys [db] :as cofx} [_ [category _ :as page]]]
    (let [{:keys [unknown pages]} db]
      (when (= category ::pages/terms)
        (if (and (not (unknown page))
                 (not (get-in pages page)))
          {:dispatch [::request page]}
          {:dispatch [::update-location page]})))))

(rf/reg-event-fx
  ::enqueue
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db] :as cofx} [_ page]]
    (let [now         (::cofx/now cofx)
          queued-page (with-meta page {:ts now})]
      {:db (update db :queue conj queued-page)})))

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
  (fn [cofx [_ [category id :as page]]]
    (let [base-uri (get-in cofx [:db :query-uri])
          uri      (str base-uri (name category) "/" id)
          queue    (:queue cofx)]
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
  (fn [{:keys [db] :as cofx} [_ result]]
    (let [content (transit/read transit-reader result)
          page    (:page content)
          now     (::cofx/now cofx)]
      {:db         (update db :queries conj {:state     :success
                                             :content   content
                                             :timestamp now})
       :dispatch-n [[::dequeue page]
                    [::save-page content]]})))

(rf/reg-event-fx
  ::on-request-failure
  [(rf/inject-cofx ::cofx/now)]
  (fn [{:keys [db] :as cofx} [_ result]]
    (let [now (::cofx/now cofx)]
      {:db (update db :queries conj {:state     :failure
                                     :content   result
                                     :timestamp now})})))

;; Successful request to the backend lead to the retrieved page being saved in
;; the frontend db. In cases where a term does not have an associated page,
;; it is registered as unknown to prevent further retrieval attempts.
(rf/reg-event-fx
  ::save-page
  (fn [_ [_ {:keys [page result]}]]
    (let [[category id] page]
      {:dispatch-n [(cond
                      (nil? result) [::save-unknown id]
                      (= category ::pages/terms) [::save-term page result])
                    [::update-location page]]})))

(rf/reg-event-db
  ::save-unknown
  (fn [db [_ term]]
    (update db :unknown conj term)))

;; Store result directly and then store individual entries.
;; TODO: reduce overwrites for hanzi result?
(rf/reg-event-db
  ::save-term
  (fn [db [_ [category id :as page] result]]
    (-> db
        (assoc-in [:pages category id] (-> result
                                           (d/reduce-result)
                                           (d/sort-result)))
        (save-dict-entries result))))

;; Dispatched either directly by ::load-page or indirectly through a successful
;; backend request. This ensures that the address bar is only updated when
;; content actually exists.
(rf/reg-event-fx
  ::update-location
  [(rf/inject-cofx ::cofx/pathname)]
  (fn [{:keys [db] :as cofx} [_ [_ id :as page]]]
    (let [{:keys [input unknown]} db
          pathname (::cofx/pathname cofx)]
      (when (and (= input id)
                 (not (unknown id))
                 (not (pages/equivalent pathname page)))
        {::fx/navigate-to (pages/page->pathname page)}))))

(rf/reg-event-fx
  ::change-location
  [(rf/inject-cofx ::cofx/now)
   (rf/inject-cofx ::cofx/scroll-states)]
  (fn [{:keys [db] :as cofx} [_ new-page]]
    (let [{:keys [input history]} db
          current-page  (first history)
          now           (::cofx/now cofx)
          scroll-states (::cofx/scroll-states cofx)]
      {:db         (-> db
                       (update :history conj (with-meta new-page now))
                       (assoc :input (or input
                                         (mk-input new-page))))
       :dispatch-n [[::save-scroll-states current-page scroll-states]
                    [::load-page (pages/shortened new-page)]]})))


;;;; ACTION CHOOSER

;; Only dispatched when the action-chooser is open.
(rf/reg-event-fx
  ::on-key-down
  (fn [{:keys [db] :as cofx} [_ key]]
    (let [{:keys [actions checked-action]} db
          next?      (fn [k] (contains? #{"ArrowRight" "ArrowDown"} k))
          prev?      (fn [k] (contains? #{"ArrowLeft" "ArrowUp"} k))
          valid-num? (fn [k] (let [num (js/parseInt k)]
                               (and (int? num)
                                    (< 0 num (inc (count actions))))))]
      (cond
        (= "Escape" key)
        (rf/dispatch [::choose-action [::close-action-chooser]])

        (= "Enter" key)
        (rf/dispatch [::choose-action (nth actions checked-action)])

        (valid-num? key)
        (let [action (nth actions (dec (js/parseInt key)))]
          (rf/dispatch [::choose-action action]))

        ;; Starts from beginning when upper bound is crossed.
        (next? key)
        (let [bound (dec (count actions))
              n     (if (< checked-action bound)
                      (inc checked-action)
                      0)]
          (rf/dispatch [::check-action n]))

        ;; Goes to last action when lower bound is crossed.
        (prev? key)
        (let [n (if (> checked-action 0)
                  (dec checked-action)
                  (dec (count actions)))]
          (rf/dispatch [::check-action n]))))))

(rf/reg-event-fx
  ::open-action-chooser
  [(rf/inject-cofx ::cofx/active-element)]
  (fn [{:keys [db] :as cofx} _]
    (let [active-element (::cofx/active-element cofx)
          actions        (:actions (first (:evaluations db)))]
      ;; Firefox won't get keydown events without removing focus from the input
      {::fx/blur active-element
       :db       (-> db
                     (assoc :checked-action 0)
                     (assoc :actions (conj actions [::close-action-chooser])))})))

(rf/reg-event-db
  ::close-action-chooser
  (fn [db _]
    (assoc db :actions nil)))

;; TODO: figure out a better way to regain focus for previously disabled field
(rf/reg-event-fx
  ::regain-input-focus
  (fn [_ _]
    {::fx/set-focus [(.getElementById js/document "input-field") 100]}))

(rf/reg-event-db
  ::check-action
  (fn [db [_ n]]
    (assoc db :checked-action n)))

;; Dispatched by user selecting an action in the action-chooser.
;; ::close-action-chooser (= cancel) is a special action (doesn't clear input).
(rf/reg-event-fx
  ::choose-action
  (fn [_ [_ action]]
    (if (= [::close-action-chooser] action)
      {:dispatch-n [[::close-action-chooser]
                    [::regain-input-focus]]}
      {:dispatch-n [[::close-action-chooser]
                    action]})))


;;;; ACTIONS (= events triggered by submitting input)

;; NOTE: actions that navigate to a new page should call ::reset-scroll-state
;; in order to avoid (the default) restoration of the scroll state.
;; It is currently not possible to distinguish between back/forward buttons
;; and other forms of navigation, so this manual invocation is necessary.
;; This is also the case for links on the site.

(rf/reg-event-fx
  ::look-up
  (fn [_ [_ term]]
    {:dispatch [::load-page [::pages/terms term]]}))

(rf/reg-event-fx
  ::digits->diacritics
  (fn [{:keys [db] :as cofx} [_ input]]
    (let [new-input (p/digits->diacritics input)]
      {:db       (assoc db :input new-input)
       :dispatch [::regain-input-focus]})))

(rf/reg-event-fx
  ::diacritics->digits
  (fn [{:keys [db] :as cofx} [_ input]]
    (let [new-input (p/diacritics->digits input)]
      {:db       (assoc db :input new-input)
       :dispatch [::regain-input-focus]})))