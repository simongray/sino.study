(ns sinostudy.views.actions
  (:require [re-frame.core :as rf]
            [sinostudy.subs :as subs]
            [sinostudy.events.core :as events]
            [sinostudy.events.actions :as actions]))

(defn- action-text
  [[action query]]
  (case action
    ::events/look-up (str "Look up " query)
    ::actions/digits->diacritics "Convert to diacritics"
    ::actions/diacritics->digits "Convert to digits"
    ::actions/close-action-chooser "Cancel"))

(defn- action-choice
  [checked action]
  (let [choose-action (fn [e]
                        (.preventDefault e)
                        (rf/dispatch [::actions/choose-action action]))]
    [:li {:key action}
     [:input {:type      :radio
              :name      "action"
              :value     action
              :checked   (= action checked)
              :id        action
              :on-change choose-action}]
     [:label {:for      action
              :on-click choose-action}
      (action-text action)]]))

(defn action-chooser []
  "The pop-in dialog that is used to select from different possible options."
  (let [actions @(rf/subscribe [::subs/actions])
        checked @(rf/subscribe [::subs/checked-action])]
    (when actions
      [:fieldset#actions
       [:legend "Select an action"]
       [:ol
        (map (partial action-choice (nth actions checked)) actions)]])))
