(ns sinostudy.db
  (:require [sinostudy.pages.core :as pages]
            [sinostudy.dictionary.core :as d]))

(def static-pages
  {"/404"      [:main
                [:article.full
                 [:h1 "Sorry,"]
                 [:p "but that page doesn't exist."]]]
   "/"         [:main#splash
                [:img {:src "/img/logo_dark_min.svg"}]
                [:blockquote
                 "... a modern Chinese dictionary and grammar tool. "
                 "Here you can look up unknown words or find out what is going on in a sentence. "
                 [:a {:href  "/about"
                      :title "Learn more about sino.study"}
                  "Learn More."]]]
   "/about"    [:main
                [:article.full
                 [:h1 "About"]
                 [:p "This is the About page."]]]
   "/settings" [:main
                [:article.full
                 [:h1 "Settings"]
                 [:p "This is the Settings page."]]]})

(def default-db
  {:input          nil
   :actions        nil
   :checked-action 0
   :script         ::d/simplified
   :result-filters {}
   :unknown        #{}
   :pages          {::pages/terms  {}
                    ::pages/static static-pages}
   :history        '()
   :scroll-states  {}
   :evaluations    '()
   :queries        '()})
