(defproject sinostudy "0.1.0-SNAPSHOT"
  :description "The sino.study project."
  :url "http://sino.study"
  :min-lein-version "2.5.3"
  :source-paths ["src/clj" "src/cljc"]

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; reagent/re-frame
                 [org.clojure/clojurescript "1.9.908"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.2"]
                 [day8.re-frame/http-fx "0.1.4"]

                 ;; compojure
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]]

            ;; reagent/re-frame
  :plugins [[lein-cljsbuild "1.1.5"]

            ;; compojure
            [lein-ring "0.9.7"]]

                                 ;; reagent/re-frame
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.4"]
                                  [re-frisk "0.5.2"]

                                  ;; compojure
                                  [javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]]

                             ;; reagent/re-frame
                   :plugins [[lein-figwheel "0.5.13"]]}}

  ;; compojure
  :ring {:handler sinostudy.handler/app}

  ;; reagent/re-frame
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "sinostudy.core/mount-root"}
                        :compiler     {:main                 sinostudy.core
                                       :output-to            "resources/public/js/compiled/app.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :asset-path           "js/compiled/out"
                                       :source-map-timestamp true
                                       :preloads             [devtools.preload re-frisk.preload]
                                       :external-config      {:devtools/config {:features-to-install :all}}}}

                       {:id           "min"
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main            sinostudy.core
                                       :output-to       "resources/public/js/compiled/app.js"
                                       :optimizations   :advanced
                                       :closure-defines {goog.DEBUG false}
                                       :pretty-print    false}}]})
