(ns sinostudy.site)

(defn current-year []
  #?(:clj  (.getYear (java.time.LocalDateTime/now))
     :cljs (.getFullYear (js/Date.))))
