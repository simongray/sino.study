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
  ::page-history
  (fn [_]
    [(rf/subscribe [::pages])])
  (fn [[pages]]
    (:history pages)))

(rf/reg-sub
  ::page
  (fn [_]
    [(rf/subscribe [::pages])
     (rf/subscribe [::page-history])])
  (fn [[pages page-history]]
    (let [current-page (first page-history)]
      (when (not (nil? current-page))
        (get-in pages current-page)))))

(rf/reg-sub
  ::page?
  (fn [_]
    [(rf/subscribe [::page])])
  (fn [[page]]
    (not (nil? page))))

(rf/reg-sub
  ::page-content
  (fn [_]
    [(rf/subscribe [::page])])
  (fn [[page]]
    (when page
      (:content page))))

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
