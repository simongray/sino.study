(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.db :refer [static-db]]))

(rf/reg-sub
  ::button-label
  (fn [db]
    (:button-label db)))

(rf/reg-sub
  ::input
  (fn [db]
    (:input db)))

(rf/reg-sub
  ::typing?
  (fn [db]
    (not (empty? (:input db)))))

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
  ::current-page
  (fn [_]
    [(rf/subscribe [::pages])
     (rf/subscribe [::history])])
  (fn [[pages history]]
    (let [[current-page _] (first history)]
      ;; non-existing pages revert to the home page by returning nil
      (get-in pages current-page))))

;; the currently active link in the nav section
(rf/reg-sub
  ::current-nav
  (fn [_]
    [(rf/subscribe [::history])])
  (fn [[history]]
    (let [[[page-category key] _] (first history)]
      (if (= page-category :static)
        key
        "/"))))

(rf/reg-sub
  ::page-content
  (fn [_]
    [(rf/subscribe [::current-page])])
  (fn [[page]]
    (when page
      (:content page))))

(rf/reg-sub
  ::page-key
  (fn [_]
    [(rf/subscribe [::history])])
  (fn [[history]]
    (let [[[page-category key] _] (first history)]
      (str page-category key))))

(rf/reg-sub
  ::input-css-class
  (fn [_]
    [(rf/subscribe [::evaluation])])
  (fn [[evaluation]]
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
  ::hint-content
  (fn [_]
    [(rf/subscribe [::hint])])
  (fn [[hint]]
    (let [hint-type (if hint (:type hint) :default)]
      (get (:hint-content static-db) hint-type))))

(rf/reg-sub
  ::hint-key
  (fn [_]
    [(rf/subscribe [::hint])])
  (fn [[hint]]
    (:type hint)))

(rf/reg-sub
  ::queries
  (fn [db]
    (:queries db)))
