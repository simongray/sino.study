(ns sinostudy.db
  (:require [sinostudy.pages.core :as pages]
            [sinostudy.dictionary.core :as d]))

(def static-pages
  {"/help"  [:div [:h1 "Help"] [:p "This is the Help page."]]
   "/blog"  [:div [:h1 "Blog"] [:p "This is the Blog page."]]
   "/about" [:div [:h1 "About"] [:p "This is the About page."]]})

(def default-db
  {:input          ""
   :actions        nil
   :marked-action  0
   :script         ::d/simplified
   :result-filters {}
   :pages          {::pages/terms  {}
                    ::pages/static static-pages}
   :history        '()
   :evaluations    '()
   :hints          '()
   :queries        '()})
