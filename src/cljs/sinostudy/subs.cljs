(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.pages.core :as pages]
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
  ::unknown
  (fn [db]
    (:unknown db)))

(rf/reg-sub
  ::history
  (fn [db]
    (:history db)))

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
  ::checked-action
  (fn [db]
    (:checked-action db)))

(rf/reg-sub
  ::current-evaluation
  (fn [db]
    (first (:evaluations db))))

;; Searches history to get the latest page that has any content available.
;; If a new page is added to the page history stack, but doesn't yet have any
;; content available, the current page will not change before it does.
;; If no content exists (i.e. an unknown term) then it also stays the same.
(rf/reg-sub
  ::current-page
  (fn [_]
    [(rf/subscribe [::pages])
     (rf/subscribe [::history])])
  (fn [[pages full-history]]
    (loop [history full-history]
      (let [[page _] (first history)
            content (get-in pages (pages/shortened page))]
        (when page
          (if content
            page
            (recur (rest history))))))))

(rf/reg-sub
  ::current-category
  (fn [_]
    (rf/subscribe [::current-page]))
  (fn [page]
    (first page)))

(rf/reg-sub
  ::current-id
  (fn [_]
    (rf/subscribe [::current-page]))
  (fn [page]
    (second page)))

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

;; The result filters are stored in a map with pages as keys.
(rf/reg-sub
  ::result-filters
  (fn [db]
    (:result-filters db)))

(rf/reg-sub
  ::current-result-types
  (fn [_]
    [(rf/subscribe [::current-category])
     (rf/subscribe [::content])])
  (fn [[category content]]
    (when (and (= category ::pages/terms)
               (not (contains? content ::d/uses)))
      (->> (keys content)
           (filter (partial not= ::d/term))
           (sort)))))

(rf/reg-sub
  ::current-result-filter
  (fn [_]
    [(rf/subscribe [::current-category])
     (rf/subscribe [::content])
     (rf/subscribe [::result-filters])
     (rf/subscribe [::current-result-types])])
  (fn [[category
        {search-term ::d/term
         :as         content}
        result-filter
        current-result-types]]
    (when (= category ::pages/terms)
      (or (get result-filter search-term)
          (apply max-key (comp count (partial get content))
                 current-result-types)))))

;; the currently active link in the nav section
;; used to determine which top-level link to disable
(rf/reg-sub
  ::current-nav
  (fn [_]
    (rf/subscribe [::current-page]))
  (fn [[page-type key]]
    (when (= page-type ::pages/static) key)))
