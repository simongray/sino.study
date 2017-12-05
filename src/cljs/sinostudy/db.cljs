(ns sinostudy.db)

(def default-db
  {:button-label "go"
   :input        ""
   :pages        {:current nil
                  :analyses {}
                  :words {}
                  :tests {}}
   :evaluations  '()
   :hints        '()
   :queries      '()})

(defn press-enter-to [s]
  [:div "press " [:span.keypress "enter"] " to " s])

(def static-db
  {:hint-content
   {:examining          "examining..."
    :query-failure      "error!"
    :no-actions         "not sure what to do with that..."
    :digits->diacritics (press-enter-to "convert to tone diacritics")
    :diacritics->digits (press-enter-to "convert to tone digits")
    :analyse-text       (press-enter-to "get an analysis")
    :look-up            (press-enter-to "look up the word")
    :choose-action      (press-enter-to "choose an action")
    :default            ""}})
