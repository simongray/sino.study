(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.db :as db]))

(rf/reg-sub
  ::input
  (fn [db]
    (:input db)))

(rf/reg-sub
  ::evaluation
  (fn [db]
    (first (:evaluations db))))

(rf/reg-sub
  ::pages
  (fn [db]
    (:pages db)))

(rf/reg-sub
  ::history
  (fn [db]
    (:history db)))

(rf/reg-sub
  ::page
  (fn [_]
    (rf/subscribe [::history]))
  (fn [history]
    (let [[page _] (first history)]
      page)))

;; the currently active link in the nav section
;; used to determine which top-level link to disable
(rf/reg-sub
  ::nav
  (fn [_]
    (rf/subscribe [::page]))
  (fn [[page-type key]]
    (when (= page-type :static) key)))

(rf/reg-sub
  ::page-content
  (fn [_]
    [(rf/subscribe [::page])
     (rf/subscribe [::pages])])
  (fn [[page pages]]
    (when page
      (let [page-type    (first page)
            page-content (get-in pages page)]
        (case page-type
          :static (:content page-content)
          :word [:p (str page-content)])))))                ;TODO: make it look nice

(rf/reg-sub
  ::page-key
  (fn [_]
    (rf/subscribe [::page]))
  (fn [[page-category key]]
    (str page-category key)))

;; controls whether the input bar is coloured
(rf/reg-sub
  ::input-css-class
  (fn [_]
    (rf/subscribe [::evaluation]))
  (fn [evaluation]
    (if (and evaluation
             (empty? (:actions evaluation))
             (not= "" (:query evaluation)))
      "default no-actions"
      "default")))

(rf/reg-sub
  ::hint
  (fn [db]
    (first (:hints db))))

(rf/reg-sub
  ::hint-type
  (fn [_]
    (rf/subscribe [::hint]))
  (fn [hint]
    (if hint
      (:type hint)
      :default)))

(rf/reg-sub
  ::hint-content
  (fn [_]
    (rf/subscribe [::hint-type]))
  (fn [hint-type]
    (get db/hint-contents hint-type)))

(rf/reg-sub
  ::queries
  (fn [db]
    (:queries db)))
