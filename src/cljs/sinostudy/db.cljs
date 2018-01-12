(ns sinostudy.db)

(def static
    {"/help"  {:content-type :hiccup
               :content      [:div [:h1 "Help"] [:p "This is the Help page."]]}
     "/blog"  {:content-type :hiccup
               :content      [:div [:h1 "Blog"] [:p "This is the Blog page."]]}
     "/about" {:content-type :hiccup
               :content      [:div [:h1 "About"] [:p "This is the About page."]]}})

(def default-db
  {:input       ""
   :script      :simplified
   :pages       {:sentence {}
                 :word     {}
                 :static   static}
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
