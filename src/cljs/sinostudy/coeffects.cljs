(ns sinostudy.coeffects
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

(rf/reg-cofx
  ::now
  (fn [cofx _]
    (assoc cofx ::now (js/Date.))))

;; Retrieves scroll states for all tags defined by the selector string.
;; In the current design, the window/document itself is no longer scrollable,
;; so there is no need to retrieve its scroll state.
(rf/reg-cofx
  ::scroll-states
  (fn [cofx _]
    (let [selector      "*[id], main, body"
          elements      (array-seq (js/document.querySelectorAll selector))
          ->selector    (fn [element]
                          (->> [(.-tagName element) (.-id element)]
                               (remove nil?)
                               (str/join "#")))
          scroll-states (into {} (for [element elements]
                                   (let [x (.-scrollLeft element)
                                         y (.-scrollTop element)]
                                     (when (or (> x 0) (> y 0))
                                       [(->selector element) [x y]]))))]
      (assoc cofx ::scroll-states scroll-states))))

(rf/reg-cofx
  ::active-element
  (fn [cofx _]
    (assoc cofx ::active-element (.-activeElement js/document))))

(rf/reg-cofx
  ::pathname
  (fn [cofx _]
    (assoc cofx ::pathname (js/decodeURIComponent js/window.location.pathname))))

(rf/reg-cofx
  ::local-storage
  (fn [cofx key]
    (assoc cofx ::local-storage (js->clj (.getItem js/localStorage key)))))
