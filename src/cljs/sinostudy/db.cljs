(ns sinostudy.db)

(def default-db
  {:button-label "go"
   :input ""
   :evaluation nil ; refers directly to a CSS class name (for now)
   :queries '()}) ; using list as stack