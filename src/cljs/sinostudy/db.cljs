(ns sinostudy.db
  (:require [sinostudy.pages.defaults :as pd]))

(def static-pages
  {"/help"  [:div [:h1 "Help"] [:p "This is the Help page."]]
   "/blog"  [:div [:h1 "Blog"] [:p "This is the Blog page."]]
   "/about" [:div [:h1 "About"] [:p "This is the About page."]]})

(def default-db
  {:input       ""
   :script      :simplified
   :pages       {:sentence {}
                 pd/words  {}
                 pd/static static-pages}
   :history     '()
   :evaluations '()
   :hints       '()
   :queries     '()})

(defn press-enter-to [s]
  [:div "press " [:span.keypress "enter"] " to " s])

;; action-related hints (press-enter-to ...) must match action name!
(def hint-contents
  {:examining          "examining..."
   :query-failure      "error!"
   :no-actions         "not sure what to do with that..."
   :digits->diacritics (press-enter-to "convert to tone diacritics")
   :diacritics->digits (press-enter-to "convert to tone digits")
   :analyse-text       (press-enter-to "get an analysis")
   :look-up-word       (press-enter-to "look up the word")
   :choose-action      (press-enter-to "choose an action")
   :default            ""})
