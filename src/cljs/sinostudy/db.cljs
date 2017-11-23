(ns sinostudy.db)

(def default-db
  {:button-label "examine"
   :input ""
   :evaluation nil ; refers directly to a CSS class name (for now)
   :hint "write something in Chinese and press the button"
   :hints {:evaluating "evaluating..."
           :examining "retrieving analysis..."
           :default ""}
   :queries '()}) ; using list as stack
