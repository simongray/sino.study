(ns sinostudy.site)

(defn header []
  [:header
   [:a {:href "/"}
    [:img#logo {:src "/img/logo_min.svg"}]]])

(defn- navlink
  [from to text]
  (if (= from to)
    [:a.current-page text]
    [:a {:href to} text]))

(def current-year
  #?(:clj (.getYear (java.time.LocalDateTime/now))
     :cljs (.getFullYear (js/Date.))))

(def year-string
  (if (> current-year 2017)
    (str "2017-" current-year)
    "2017"))

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
     [:p#copyright "© " year-string " Simon Gray ("
      [:a {:href "https://github.com/simongray"} "github"]
      ")"]]))
