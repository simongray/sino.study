(ns sinostudy.state.events.scrolling
  (:require [re-frame.core :as rf]
            [sinostudy.state.effects :as fx]))

(rf/reg-event-db
  ::save-scroll-state
  (fn [db [_ page scroll-state]]
    (if (not (empty? scroll-state))
      (assoc-in db [:scroll-states page] scroll-state)
      db)))

(rf/reg-event-db
  ::reset-scroll-state
  (fn [db [_ page]]
    (update db :scroll-states dissoc page)))

(rf/reg-event-fx
  ::load-scroll-state
  (fn [{:keys [db]} [_ page]]
    {::fx/set-scroll-state (get-in db [:scroll-states page])}))
