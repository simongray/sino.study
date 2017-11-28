(ns sinostudy.db)

(def default-db
  {:button-label "go"
   :input        ""
   :evaluations  '()
   :hints        '()
   :queries      '()})

(def static-db
  {:hint-content
   {:examining          "examining..."
    :query-failure      "error!"
    :no-actions         "not sure what to do with that..."
    :digits->diacritics "press enter to convert tone digits to diacritics"
    :diacritics->digits "press enter to convert tone diacritics to digits"
    :analyse-text       "press enter to get a grammatical analysis"
    :look-up            "press enter to look up the word"
    :choose-action      "press enter to choose an action"
    :default            ""}})
