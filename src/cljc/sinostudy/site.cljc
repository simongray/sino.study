(ns sinostudy.site)

(defn header []
  [:header
   [:div#logo]])

(defn footer []
  [:footer
   [:p
    [:a {:href "/help"} "Help"]
    " · "
    [:a {:href "/about"} "About"]
    " · "
    [:a {:href "/blog"} "Blog"]
    " · © 2018 Simon Gray"]])
