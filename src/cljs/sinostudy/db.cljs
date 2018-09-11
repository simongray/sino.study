(ns sinostudy.db
  (:require [sinostudy.pages.core :as pages]
            [sinostudy.dictionary.core :as d]))

(def static-pages
  {"/about"    [:main [:article.full [:h1 "About"] [:p "This is the About page."]]]
   "/settings" [:main [:article.full [:h1 "Settings"] [:p "This is the Settings page."]]]})

(def default-db
  {:input          nil
   :actions        nil
   :checked-action 0
   :script         ::d/simplified
   :result-filters {}
   :pages          {::pages/terms  {}
                    ::pages/static static-pages}
   :history        '()
   :scroll-states  {}
   :evaluations    '()
   :queries        '()})
