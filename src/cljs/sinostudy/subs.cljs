(ns sinostudy.subs
  (:require [re-frame.core :as rf]
            [sinostudy.pages.core :as pages]
            [sinostudy.events :as events]
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

(def hint-content
  {::events/query-failure       "something went wrong..."
   ::events/no-actions          "not sure what to do with that..."
   ::events/digits->diacritics  "press enter to convert to tone diacritics"
   ::events/diacritics->digits  "press enter to convert to tone digits"
   ::events/look-up             "press enter to look up the term"
   ::events/open-action-chooser "press enter to choose an action"})

(rf/reg-sub
  ::hint
  (fn [_]
    (rf/subscribe [::hints]))
  (fn [hints]
    (get hint-content (:type (first hints)))))

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
