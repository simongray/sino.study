(ns sinostudy.site
  (:require [clojure.string :as str]))

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
  #?(:clj  (.getYear (java.time.LocalDateTime/now))
     :cljs (.getFullYear (js/Date.))))

(def year-string
  (if (> current-year 2017)
    (str "2017-" current-year)
    "2017"))

(defn footer [page]
  (fn []
    [:footer
     [:nav
      (interpose " · "
                 [(navlink page "/" "Home")
                  (navlink page "/test" "Test") ;; TODO: remove
                  (navlink page "/help" "Help")
                  (navlink page "/blog" "Blog")
                  (navlink page "/about" "About")])]
     [:p#copyright "© " year-string " Simon Gray ("
      [:a {:href "https://github.com/simongray"} "github"]
      ")"]]))
