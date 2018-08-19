(ns sinostudy.db
  (:require [sinostudy.pages.core :as pages]
            [sinostudy.dictionary.core :as d]))

(def static-pages
  {"/help"  [:<> [:h1 "Help"] [:p "This is the Help page."]]
   "/blog"  [:<> [:h1 "Blog"] [:p "This is the Blog page."]]
   "/about" [:<> [:h1 "About"] [:p "This is the About page."]]})

(def default-db
  {:input          ""
   :actions        nil
   :checked-action 0
   :script         ::d/simplified
   :result-filters {}
   :pages          {::pages/terms  {}
                    ::pages/static static-pages}
   :history        '()
   :scroll-states  {}
   :evaluations    '()
   :hints          '()
   :queries        '()})
