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

(def initial-db
  "This is the db map used as the initial state of the db."
  {;; The current contents of the text input field. Shown directly in the UI.
   ;; This usually just reflects what the user is typing in, but can also be
   ;; affected by conversion operations, e.g. `digits->diacritics`.
   :input          nil

   :query-uri      query-uri
   :pages          {::pages/terms  {}
                    ::pages/static static-pages}
   :unknown        #{}
   :history        '()
   :queue          (sorted-set-by timestamp-comparator)

   ;; Result filters are a mapping from terms to user-selected result filters.
   ;; These filters are the ones that control whether we're searching for
   ;; plain Pinyin, English, the official Pinyin with diacritics, or the popular
   ;; online version where tone diacritics have been replaced by digits.
   ;; Terms where the user never deviated from the default choice of filter
   ;; do not appear in this map, only the ones that were actively selected.
   ;; These are (in a similar fashion to :scroll-states) used to recreate UI
   ;; state when navigating back in history during the browsing session.
   :result-filters {}

   ;; A stack of maps containing evaluations, i.e. maps of input query, output
   ;; actions, and timestamp. Basically used to memoise query input to its
   ;; results to speed up recollection.
   :evaluations    '()

   ;; Holds a record of the query content. Not used for much at the moment, but
   ;; be used to see the history of backend requests and whether they were
   ;; successful or not.
   :queries        '()

   ;; The preferred script is registered here and this will simply use the
   ;; selected script over the other whenever there is an option of both in the
   ;; UI. This currently doesn't change the term *itself* on term pages, as this
   ;; would also require mutating the URL whenever the user switches the script.
   :script         ::d/simplified

   ;; Scroll states is a in-memory collection of the scroll state of the page
   ;; whenever a new page is reached. Since this is an SPA, the browser doesn't
   ;; necessarily remember how far along the page was scrolled at a specific
   ;; point in the browsing history. To remedy this, the states here can be
   ;; recreated. This is then tied in with the page navigation mechanism.
   :scroll-states  {}

   ;; The action chooser is pop-in window that can be used to select between
   ;; multiple different actions. The window appears spontaneously when a piece
   ;; user input can have multiple interpretations and the user needs to filter
   ;; it. In this case `actions` is a vector of possible actions options and
   ;; `checked-action` is the index of the currently selected option.
   :actions        nil
   :checked-action 0

   ;; This is the content of the `config.edn` file that is read at launch.
   ;; While the content is mostly relevant for the backend, this can be used
   ;; to reflect this information in the frontend UI, e.g. for debugging
   ;; purposes.
   :config         config})
