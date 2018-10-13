(ns sinostudy.pages.core)

(defn short
  "Helper function to make sure page is 2 items max."
  [page]
  (when (> (count page) 1)
    (subvec page 0 2)))
subs