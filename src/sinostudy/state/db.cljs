(ns sinostudy.state.db
  (:require [cljs.reader :as reader]
            [sinostudy.config :as cf]
            [sinostudy.navigation.pages :as pages]
            [sinostudy.dictionary.core :as d])
  (:require-macros [sinostudy.macros.core :as macros]))

(def config
  (reader/read-string (macros/slurp "resources/config.edn")))

;; TODO: fix this so that running a JAR locally will still work
(def query-uri
  (let [hostname js/window.location.hostname
        port     (if cf/debug?
                   (get-in config [:server :port :internal])
                   (get-in config [:server :port :external]))]
    (str "http://" hostname ":" port "/query/")))

;; TODO: these are views, move to appropriate ns
(def static-pages
  {"/404"      [:main
                [:article.full
                 [:h1 "Sorry,"]
                 [:p "that page doesn't exist."]]]
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

;; When used in conjunction with `sorted-set-by`, this comparator can be used to
;; get the functionality of a set, but ordered by time for occasional trimming.
(defn- timestamp-comparator
  "Compare by timestamp as set in the metadata."
  [x y]
  (let [ts (comp :ts meta)]
    (if (= x y)
      0
      (compare (ts x) (ts y)))))

(def default-db
  {:config         config
   :query-uri      query-uri
   :input          nil
   :actions        nil
   :checked-action 0
   :script         ::d/simplified
   :result-filters {}
   :unknown        #{}
   :pages          {::pages/terms  {}
                    ::pages/static static-pages}
   :history        '()
   :queue          (sorted-set-by timestamp-comparator)
   :scroll-states  {}
   :evaluations    '()
   :queries        '()})
