(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.db :as db]
            [sinostudy.events :as events]))

(rf/reg-sub
  ::input
  (fn [db]
    (:input db)))

(rf/reg-sub
  ::pages
  (fn [db]
    (:pages db)))

(rf/reg-sub
  ::history
  (fn [db]
    (:history db)))

(rf/reg-sub
  ::hints
  (fn [db]
    (:hints db)))

(rf/reg-sub
  ::queries
  (fn [db]
    (:queries db)))

(rf/reg-sub
  ::script
  (fn [db]
    (:script db)))

(rf/reg-sub
  ::decomposed
  (fn [db]
    (:decomposed db)))

(rf/reg-sub
  ::mode
  (fn [db]
    (:mode db)))

(rf/reg-sub
  ::actions
  (fn [db]
    (:actions db)))

(rf/reg-sub
  ::marked-action
  (fn [db]
    (:marked-action db)))

(rf/reg-sub
  ::current-evaluation
  (fn [db]
    (first (:evaluations db))))

;; the page is a vector describing the path to the content in (:pages db)
;; in the case of a static web page it looks like e.g. [:static "/about"]
;; for words it might look [:words "你好"] or [:words "de" 3],
;; in which case the 3 will look up the word at index 3 in the list of entries
(rf/reg-sub
  ::current-page
  (fn [_]
    (rf/subscribe [::history]))
  (fn [history]
    (let [[page _] (first history)]
      page)))

;; the currently active link in the nav section
;; used to determine which top-level link to disable
(rf/reg-sub
  ::current-nav
  (fn [_]
    (rf/subscribe [::current-page]))
  (fn [[page-type key]]
    (when (= page-type :static) key)))
