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

;; loads the actual page content
;; the page is a vector describing the path to the content in (:pages db)
;; in the case of a static web page it looks like e.g. [:static "/about"]
;; for words it might look [:word "你好"] or [:word "de" 3],
;; in which case the 3 will look up the word at index 3 in the list of entries
(rf/reg-sub
  ::page-content
  (fn [_]
    [(rf/subscribe [::page])
     (rf/subscribe [::pages])])
  (fn [[page pages]]
    (when page
      (get-in pages page))))

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

(rf/reg-sub
  ::script
  (fn [db]
    (:script db)))
