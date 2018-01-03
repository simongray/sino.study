(ns sinostudy.db
  (:require [sinostudy.site :as site]))

(def static
  (let [timestamp (site/current-year)]
    {"/help"  {:timestamp    timestamp
               :content-type :hiccup
               :content      [:div [:h1 "Help"] [:p "This is the Help page."]]}
     "/blog"  {:timestamp    timestamp
               :content-type :hiccup
               :content      [:div [:h1 "Blog"] [:p "This is the Blog page."]]}
     "/about" {:timestamp    timestamp
               :content-type :hiccup
               :content      [:div [:h1 "About"] [:p "This is the About page."]]}}))

(def default-db
  {:button-label "go"
   :input        ""
   :pages        {:analyses {}
                  :words    {}
                  :static   static
                  :tests    {}}
   :history      '()
   :evaluations  '()
   :hints        '()
   :queries      '()})

(defn press-enter-to [s]
  [:div "press " [:span.keypress "enter"] " to " s])

(def hint-contents
  {:examining          "examining..."
   :query-failure      "error!"
   :no-actions         "not sure what to do with that..."
   :digits->diacritics (press-enter-to "convert to tone diacritics")
   :diacritics->digits (press-enter-to "convert to tone digits")
   :analyse-text       (press-enter-to "get an analysis")
   :look-up            (press-enter-to "look up the word")
   :choose-action      (press-enter-to "choose an action")
   :default            ""})
