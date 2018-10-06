(ns sinostudy.pages.core
  "This namespace contains functions related to the page abstraction used in
  sino.study, as well as serving as a namespaced keyword prefix for the various
  page categories in use, e.g. ::pages/terms."
  (:require [clojure.string :as str]))

(defn shortened
  "Helper function to make sure a page is maximum 2 items (category and id).
  Additional items do affect how a page is displayed, but still refer to the
  same basic data as the 2-item page."
  [page]
  (when (and page (> (count page) 1))
    (subvec page 0 2)))

(defn page->pathname
  "Convert a page to a window.location.pathname."
  [page]
  (str "/" (str/join "/" (map name page))))

(defn equivalent
  "Is the window.location.pathname equivalent to the given page?"
  [pathname page]
  (= pathname (page->pathname page)))
