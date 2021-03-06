(ns sinostudy.fx
  (:require [re-frame.core :as rf]
            [accountant.core :as accountant]))

;; Dispatched by actions that need to change the page (and browser history).
(rf/reg-fx
  ::navigate-to
  (fn [path]
    (accountant/navigate! path)))

;; Dispatched by ::close-action-chooser.
;; This is definitely a less than optimal solution...
(rf/reg-fx
  ::set-focus
  (fn [[element delay]]
    (js/setTimeout
      #(.focus element)
      delay)))

;; Dispatched by ::close-action-chooser.
;; This is definitely a less than optimal solution...
(rf/reg-fx
  ::blur
  (fn [element]
    (when element
      (.blur element))))

;; Dispatched by ::load-scroll-state.
(rf/reg-fx
  ::set-scroll-state
  (fn [scroll-state]
    (doseq [[css-selector [x y]] scroll-state]
      (let [element (aget (js/document.querySelectorAll css-selector) 0)]
        (set! (.-scrollLeft element) x)
        (set! (.-scrollTop element) y)))))
