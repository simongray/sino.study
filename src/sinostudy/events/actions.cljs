(ns sinostudy.events.actions
  "For all events relating to actions triggered through the text input field,
  including displaying and navigating the `action-chooser`."
  (:require [re-frame.core :as rf]
            [sinostudy.pinyin.core :as p]
            [sinostudy.cofx :as cofx]
            [sinostudy.fx :as fx]))

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
  (fn [{:keys [db ::cofx/active-element] :as cofx} _]
    (let [actions (:actions (first (:evaluations db)))]
      ;; Firefox won't get keydown events without removing focus from the input
      {::fx/blur active-element
       :db       (-> db
                     (assoc :checked-action 0)
                     (assoc :actions (conj actions [::close-action-chooser])))})))

(rf/reg-event-db
  ::close-action-chooser
  (fn [db _]
    (assoc db :actions nil)))

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

;; TODO: figure out a better way to regain focus for previously disabled field
(rf/reg-event-fx
  ::regain-input-focus
  (fn [_ _]
    {::fx/set-focus [(.getElementById js/document "input-field") 100]}))

(rf/reg-event-fx
  ::digits->diacritics
  (fn [{:keys [db] :as cofx} [_ input]]
    {:db       (assoc db :input (p/digits->diacritics input))
     :dispatch [::regain-input-focus]}))

(rf/reg-event-fx
  ::diacritics->digits
  (fn [{:keys [db] :as cofx} [_ input]]
    {:db       (assoc db :input (p/diacritics->digits input))
     :dispatch [::regain-input-focus]}))
