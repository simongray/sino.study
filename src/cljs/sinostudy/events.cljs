(ns sinostudy.events
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [cljs.reader :as reader]
            [re-frame.core :as rf]
            [accountant.core :as accountant]
            [sinostudy.db :as db]
            [sinostudy.pinyin.core :as p]
            [sinostudy.pinyin.eval :as pe]
            [sinostudy.dictionary.core :as d]
            [sinostudy.pages.core :as pages]
            [ajax.core :as ajax]
            [cognitect.transit :as transit])
  (:require-macros [sinostudy.macros.core :as macros]))

(def config
  (reader/read-string (macros/slurp "resources/config.edn")))

;; During development, frontend and backend are served on different ports,
;; but in production the backend is served on the same host and port.
(def query-uri
  (let [hostname js/window.location.hostname
        port     (:port config)]
    (str "http://" hostname ":" port "/query/")))

;; all responses from the Compojure backend are Transit-encoded
(def transit-reader
  (transit/reader :json))

(defn dictionary-preprocess
  "Process result coming from the web service before storing locally,
  e.g. presort word lists and the like."
  [result]
  (-> result
      (d/reduce-result)
      (d/sort-result)))

(defn save-dict-entries
  "Save the individual entries of a dictionary search result into the db.
  Note: this is a separate step from saving the search result itself!"
  [db content]
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
          (recur (assoc-in db* path* entry) (rest entries*)))))))

(defn mk-input
  "What the input field should display based on a given page."
  [[category id]]
  (cond
    (= ::pages/terms category) (when (not (pe/hanzi-block? id)) id)))

;;;; QUERY EVALUATION

;; pinyin sentences with tone digits can converted to diacritics,
;; but the action shouldn't appear if the sentence contains no tone digits!
(defn- digits->diacritics?
  [query]
  (and (pe/pinyin+digits+punct? query)
       (not (pe/pinyin+punct? query))))

(defn- diacritics->digits?
  [query]
  (and (pe/pinyin+diacritics+punct? query)
       (not (pe/pinyin+punct? query))))

(defn- pinyin-block?
  [query]
  (or (pe/pinyin-block? query)
      (pe/pinyin-block+digits? query)
      (pe/pinyin-block+diacritics? query)))

(defn- command?
  [query]
  (str/starts-with? query "/"))

(defn- eval-command
  "Evaluate a command query to get a vector of possible actions."
  [query]
  (case (str/lower-case query)
    "/clear" [[::initialize-db]]
    []))

(defn- eval-term
  "Evaluate a word/Pinyin query to get a vector of possible actions."
  [query]
  (let [query* (p/umlaut query)]
    (cond-> [[::look-up query]]
            (and (pinyin-block? query*)
                 (not= query query*)) (conj [::look-up (d/pinyin-key query*)])
            (digits->diacritics? query*) (conj [::digits->diacritics query*])
            (diacritics->digits? query*) (conj [::diacritics->digits query*]))))

(defn eval-query
  "Evaluate a query string to get a vector of possible actions."
  [query]
  (cond
    (command? query) (eval-command query)
    (pe/hanzi-block? query) [[::look-up query]]
    (re-find #"^\w" query) (eval-term query)))

;;;; CO-EFFECTS

(rf/reg-cofx
  ::now
  (fn [cofx _]
    (assoc cofx :now (js/Date.))))

;; Retrieves scroll states for all tags defined by the selector string.
;; In the current design, the window/document itself is no longer scrollable,
;; so there is no need to retrieve its scroll state.
(rf/reg-cofx
  ::scroll-states
  (fn [cofx _]
    (let [selector      "*[id], main, body"
          elements      (array-seq (js/document.querySelectorAll selector))
          ->selector    (fn [element]
                          (->> [(.-tagName element) (.-id element)]
                               (remove nil?)
                               (str/join "#")))
          scroll-states (into {} (for [element elements]
                                   (let [x (.-scrollLeft element)
                                         y (.-scrollTop element)]
                                     (when (or (> x 0) (> y 0))
                                       [(->selector element) [x y]]))))]
      (assoc cofx :scroll-states scroll-states))))

(rf/reg-cofx
  ::active-element
  (fn [cofx _]
    (assoc cofx :active-element (.-activeElement js/document))))

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

;; Dispatched by ::close-action-chooser.
;; This is definitely a less than optimal solution...
(rf/reg-fx
  :set-focus
  (fn [[element delay]]
    (js/setTimeout
      #(.focus element)
      delay)))

;; Dispatched by ::close-action-chooser.
;; This is definitely a less than optimal solution...
(rf/reg-fx
  :blur
  (fn [element]
    (when element
      (.blur element))))

;; Dispatched by ::load-scroll-state.
(rf/reg-fx
  :set-scroll-states
  (fn [scroll-states]
    (doseq [[k [x y]] scroll-states]
      (let [element (aget (js/document.querySelectorAll k) 0)]
        (set! (.-scrollLeft element) x)
        (set! (.-scrollTop element) y)))))

;;;; EVENTS

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(rf/reg-event-db
  ::save-scroll-states
  (fn [db [_ page scroll-states]]
    (assoc-in db [:scroll-states page] scroll-states)))

(rf/reg-event-fx
  ::reset-scroll-state
  (fn [_ [_ page]]
    {:dispatch [::save-scroll-states page nil]}))

;; This event is dispatched by the content-page component on updates.
(rf/reg-event-fx
  ::load-scroll-state
  (fn [cofx [_ page]]
    (let [scroll-states (get-in cofx [:db :scroll-states page])]
      {:set-scroll-states scroll-states})))

;; dispatched by both ::evaluate-input and ::on-study-button-press
(rf/reg-event-fx
  ::save-evaluation
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ query actions]]
    (let [db           (:db cofx)
          evaluations  (:evaluations db)
          now          (:now cofx)
          evaluations* (conj evaluations {:id        (count evaluations)
                                          :query     query
                                          :actions   actions
                                          :timestamp now})]
      {:db (assoc db :evaluations evaluations*)})))

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
          query             (str/trim input)
          new-query?        (not= query (:query latest-evaluation))
          actions           (eval-query query)]
      (when (and latest-input? new-query?)
        {:dispatch-n [[::save-evaluation query actions]
                      (when (and actions
                                 (= ::look-up (-> actions first first)))
                        (first actions))]}))))

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
      (if (str/blank? input)
        (assoc fx :dispatch [::evaluate-input input])
        (assoc fx :dispatch-later [{:dispatch [::evaluate-input input]
                                    :ms       400}])))))

;; dispatched by ::on-query-success
(rf/reg-event-db
  ::save-page
  (fn [db [_ {:keys [page result]}]]
    (let [path      (into [:pages] page)
          page-type (first page)]
      (cond
        ;; Store result directly and then store individual entries.
        ;; TODO: reduce overwrites for hanzi result?
        (= page-type ::pages/terms)
        (-> db
            (assoc-in path (dictionary-preprocess result))
            (save-dict-entries result))

        :else db))))

;; dispatched upon a successful retrieval of a query result
(rf/reg-event-fx
  ::on-query-success
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db      (:db cofx)
          queries (:queries db)
          content (transit/read transit-reader result)
          now     (:now cofx)]
      {:db       (assoc db :queries (conj queries {:id        (count queries)
                                                   :state     :success
                                                   :content   content
                                                   :timestamp now}))
       :dispatch [::save-page content]})))

;; dispatched upon an unsuccessful retrieval of a query result
(rf/reg-event-fx
  ::on-query-failure
  [(rf/inject-cofx ::now)]
  (fn [cofx [_ result]]
    (let [db      (:db cofx)
          queries (:queries db)
          now     (:now cofx)]
      {:db (assoc db :queries (conj queries {:id        (count queries)
                                             :state     :failure
                                             :content   result
                                             :timestamp now}))})))

;; dispatched by ::load-content
(rf/reg-event-fx
  ::send-query
  (fn [_ [_ query]]
    (let [default-request {:method          :get
                           :timeout         5000
                           :response-format (ajax/text-response-format)
                           :on-success      [::on-query-success]
                           :on-failure      [::on-query-failure]}
          query-type      (name (first query))
          query-string    (second query)
          uri             (str query-uri query-type "/" query-string)
          request         (assoc default-request :uri uri)]
      {:http-xhrio request})))

;; Dispatched by the user pressing keys on the keyboard.
;; Only dispatched when the action-chooser is open.
(rf/reg-event-fx
  ::on-key-down
  (fn [cofx [_ key]]
    (let [db         (:db cofx)
          actions    (:actions db)
          checked    (:checked-action db)
          next?      (fn [k] (contains? #{"ArrowRight" "ArrowDown"} k))
          prev?      (fn [k] (contains? #{"ArrowLeft" "ArrowUp"} k))
          valid-num? (fn [k] (let [num (js/parseInt k)]
                               (and (int? num)
                                    (< 0 num (inc (count actions))))))]
      (cond
        (= "Escape" key)
        (rf/dispatch [::choose-action [::close-action-chooser]])

        (= "Enter" key)
        (rf/dispatch [::choose-action (nth actions checked)])

        (valid-num? key)
        (let [action (nth actions (dec (js/parseInt key)))]
          (rf/dispatch [::choose-action action]))

        ;; Starts from beginning when upper bound is crossed.
        (next? key)
        (let [bound (dec (count actions))
              n     (if (< checked bound) (inc checked) 0)]
          (rf/dispatch [::check-action n]))

        ;; Goes to last action when lower bound is crossed.
        (prev? key)
        (let [n (if (> checked 0) (dec checked) (dec (count actions)))]
          (rf/dispatch [::check-action n]))))))

;; dispatched by ::on-submit when there are >1 actions based on query eval
(rf/reg-event-fx
  ::open-action-chooser
  [(rf/inject-cofx ::active-element)]
  (fn [cofx _]
    (let [db             (:db cofx)
          active-element (:active-element cofx)
          actions        (:actions (first (:evaluations db)))]
      ;; Firefox won't get keydown events without removing focus from the input
      {:blur active-element
       :db   (-> db
                 (assoc :checked-action 0)
                 (assoc :actions (conj actions [::close-action-chooser])))})))

;; dispatched by ::choose-action
(rf/reg-event-fx
  ::close-action-chooser
  (fn [cofx _]
    (let [db (:db cofx)]
      {:db (assoc db :actions nil)})))

;; TODO: figure out a better way to regain focus for previously disabled field
;; dispatched by ::choose-action
(rf/reg-event-fx
  ::regain-input-focus
  (fn [_ _]
    {:set-focus [(.getElementById js/document "input-field") 100]}))

;; dispatched by ::choose-action
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
      {:dispatch-n (conj [[::close-action-chooser] action])})))

(rf/reg-event-db
  ::decompose-char
  (fn [db [_ decomposition]]
    (assoc db :decomposed decomposition)))

;; dispatched by pressing enter (defined in ::on-key-down)
;; forces an evaluation for the latest input if it hasn't been evaluated yet
(rf/reg-event-fx
  ::submit
  (fn [cofx [_ input]]
    (let [db                (:db cofx)
          latest-evaluation (first (:evaluations db))
          query             (str/trim input)
          new-query?        (not= query (:query latest-evaluation))
          actions           (if new-query?
                              (eval-query query)
                              (:actions latest-evaluation))]
      {:dispatch-n
       [(when new-query? [::save-evaluation query actions])
        (case (count actions)
          1 (let [[kind query :as action] (first actions)]
              (if (= kind ::look-up)
                [::look-up query true]                      ; true = input looses focus
                action))
          [::open-action-chooser])]})))

;; dispatched by ::change-page
(rf/reg-event-fx
  ::load-content
  (fn [cofx [_ page]]
    (let [db       (:db cofx)
          pages    (:pages db)
          category (first page)]
      (cond
        (= ::pages/static category) {}
        (= ::pages/terms category) (let [dict-page (subvec page 0 2)]
                                     (if (not (get-in pages dict-page))
                                       {:dispatch [::send-query dict-page]}
                                       {}))))))

;; Dispatched by clicking links only!
;; It's never dispatched directly, as we want to leave a browser history trail.
;; Link-clicking is facilitated by frontend routing (secretary + Accountant).
(rf/reg-event-fx
  ::change-page
  [(rf/inject-cofx ::now)
   (rf/inject-cofx ::scroll-states)]
  (fn [cofx [_ new-page]]
    (let [db            (:db cofx)
          input         (:input db)
          history       (:history db)
          current-page  (when (not (empty? history))
                          (-> history first first))
          timestamp     (:now cofx)
          scroll-states (:scroll-states cofx)]
      {:db         (-> db
                       (assoc :history (conj history [new-page timestamp]))
                       (assoc :input (or input (mk-input new-page))))
       :dispatch-n [[::save-scroll-states current-page scroll-states]
                    [::load-content new-page]]})))

;; Dispatched by on-click handlers in various places.
(rf/reg-event-db
  ::change-script
  (fn [db [_ script]]
    (assoc db :script script)))

;; Dispatched by filter radio buttons in dictionary search result.
(rf/reg-event-db
  ::set-result-filter
  (fn [db [_ term type]]
    (assoc-in db [:result-filters term] type)))

;;;; ACTIONS (= events triggered by submitting input)

;; NOTE: actions that navigate to a new page should call ::reset-scroll-state
;; in order to avoid (the default) restoration of the scroll state.
;; It is currently not possible to distinguish between back/forward buttons
;; and other forms of navigation, so this manual invocation is necessary.
;; This is also the case for links on the site.

(rf/reg-event-fx
  ::look-up
  [(rf/inject-cofx ::active-element)]
  (fn [cofx [_ term submit?]]
    (let [active-element (:active-element cofx)]
      {:navigate-to (str "/" (name ::pages/terms) "/" term)
       :blur        (when submit? active-element)
       :dispatch    [::reset-scroll-state [::pages/terms term]]})))

(rf/reg-event-fx
  ::digits->diacritics
  (fn [cofx [_ input]]
    (let [db        (:db cofx)
          new-input (p/digits->diacritics input)]
      {:db       (assoc db :input new-input)
       :dispatch [::regain-input-focus]})))

(rf/reg-event-fx
  ::diacritics->digits
  (fn [cofx [_ input]]
    (let [db        (:db cofx)
          new-input (p/diacritics->digits input)]
      {:db       (assoc db :input new-input)
       :dispatch [::regain-input-focus]})))
