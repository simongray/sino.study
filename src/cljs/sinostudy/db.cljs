(ns sinostudy.db)

(def default-db
  {:button-label "go"
   :input ""
   :input-placeholder ""
   :evaluation nil ; refers directly to a CSS class name (for now)
   :hints '()
   :hint-contents {:evaluating    "evaluating..."
                   :examining     "retrieving analysis..."
                   :query-failure "query failed!"
                   :default       ""}
   :queries '()})
