(ns sinostudy.site)

(defn header []
  [:header
   [:a {:href "/"}
    [:img#logo {:src "/img/logo_min.svg"}]]])

(defn navlink
  [from to text]
  (if (not= from to)
    [:a {:href to} text]
    [:a.current-page text]))


(defn footer [page]
  (fn []
    [:footer
     [:p
      (navlink page "/" "Home")
      " · "
      (navlink page "/help" "Help")
      " · "
      (navlink page "/about" "About")
      " · "
      (navlink page "/blog" "Blog")]
     [:p#copyright "© 2018 Simon Gray ("
      [:a {:href "https://github.com/simongray"} "github"]
      ")"]]))
