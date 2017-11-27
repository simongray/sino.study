(ns sinostudy.db)

(def default-db
  {:button-label "go"
   :input ""
   :input-placeholder ""
   :evaluation nil ; refers directly to a CSS class name (for now)
   :hints '()
   :hint-contents {:writing "writing..."
                   :examining "examining..."
                   :query-failure "error!"
                   :awaiting-action "press enter to"
                   :default ""}
   :actions {:digits->diacritics "convert tone digits to diacritics"
             :diacritics->digits "convert tone diacritics to digits"
             :analyse-text "get a grammatical analysis"
             :look-up-word "look up word in the dictionary"
             :choose-action "choose an action"
             :default "submit input"}
   :queries '()})
