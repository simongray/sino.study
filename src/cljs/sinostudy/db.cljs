(ns sinostudy.db)

(def default-db
  {:button-label "examine"
   :input ""
   :evaluation nil ; refers directly to a CSS class name (for now)
   :hint "Write something in Chinese and press the button"
   :queries '()}) ; using list as stack
