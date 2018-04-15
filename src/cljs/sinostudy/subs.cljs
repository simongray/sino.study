(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.dictionary.core :as d]))

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

(rf/reg-sub
  ::current-page
  (fn [_]
    (rf/subscribe [::history]))
  (fn [history]
    (let [[page _] (first history)]
      (when (> (count page) 1)
        (subvec page 0 2)))))

(rf/reg-sub
  ::current-category
  (fn [_]
    (rf/subscribe [::current-page]))
  (fn [page]
    (first page)))

(rf/reg-sub
  ::current-attribute
  (fn [_]
    (rf/subscribe [::history]))
  (fn [history]
    (let [[page _] (first history)]
      (when (> (count page) 2)
        (get page 2)))))

(rf/reg-sub
  ::content
  (fn [_]
    [(rf/subscribe [::pages])
     (rf/subscribe [::current-page])])
  (fn [[pages page]]
    (when page
      (get-in pages page))))

(rf/reg-sub
  ::result-filters
  (fn [db]
    (:result-filters db)))

(rf/reg-sub
  ::current-result-types
  (fn [_]
    (rf/subscribe [::content]))
  (fn [content]
    (->> (keys content)
         (filter (partial not= ::d/term))
         (sort))))

(rf/reg-sub
  ::current-result-filter
  (fn [_]
    [(rf/subscribe [::content])
     (rf/subscribe [::result-filters])
     (rf/subscribe [::current-result-types])])
  (fn [[{search-term ::d/term
         :as         content}
        result-filter
        current-result-types]]
    (or (get result-filter search-term)
        (apply max-key (comp count (partial get content))
               current-result-types))))

;; the currently active link in the nav section
;; used to determine which top-level link to disable
(rf/reg-sub
  ::current-nav
  (fn [_]
    (rf/subscribe [::current-page]))
  (fn [[page-type key]]
    (when (= page-type :static) key)))
