(ns sinostudy.db
  (:require [sinostudy.pages.defaults :as pd]))

(def static-pages
  {"/help"  [:div [:h1 "Help"] [:p "This is the Help page."]]
   "/blog"  [:div [:h1 "Blog"] [:p "This is the Blog page."]]
   "/about" [:div [:h1 "About"] [:p "This is the About page."]]})

(def default-db
  {:input         ""
   :actions       nil
   :marked-action 0
   :script        :simplified
   :pages         {pd/words  {}
                   pd/static static-pages}
   :history       '()
   :evaluations   '()
   :hints         '()
   :queries       '()})
