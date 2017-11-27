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
                   :default ""}
   :queries '()})
