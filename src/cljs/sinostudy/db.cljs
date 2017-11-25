(ns sinostudy.db)

(def default-db
  {:button-label "examine"
   :input ""
   :input-placeholder "e.g. 我爱学习中文。"
   :evaluation nil ; refers directly to a CSS class name (for now)
   :hint "write something in Chinese and press the button"
   :hint-types {:evaluating    "evaluating..."
                :examining     "retrieving analysis..."
                :query-failure "query failed!"
                :default       ""}
   :queries '()})
