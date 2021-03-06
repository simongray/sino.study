(defproject sinostudy "_"
  :description "The sino.study project."
  :url "http://sino.study"
  :min-lein-version "2.8.1"
  :source-paths ["src"]
  :resource-paths ["resources"]
  :jar-name "sinostudy.jar"
  :uberjar-name "sinostudy-standalone.jar"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/test.check "0.10.0"]
                 [computerese "0.1.0-SNAPSHOT"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.2"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.8"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [clj-commons/secretary "1.2.4"]
                 [venantius/accountant "0.2.4"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [ring/ring-defaults "0.3.2"]
                 [clj-json "0.5.3"]]

  :plugins [[me.arrdem/lein-git-version "2.0.8"]
            [lein-cljsbuild "1.1.7"]]

  :git-version {:version-file      "resources/version.edn"
                :version-file-keys [:tag                    ; Name of the last git tag if any
                                    :ahead                  ; Number of commits ahead of the last tag, or 0
                                    :ahead?                 ; Is the head ahead by more than 0 commits
                                    :ref                    ; The full current ref
                                    :ref-short              ; The "short" current ref
                                    :branch                 ; The name of the current branch
                                    :dirty?                 ; Optional. Boolean. Are there un-committed changes.
                                    :message                ; Optional. The last commit message when clean.
                                    :timestamp]}            ; Optional. The last commit date when clean.]}

  :profiles {:dev     {:dependencies [[binaryage/devtools "0.9.10"]
                                      [day8.re-frame/re-frame-10x "0.4.2"]
                                      [figwheel-sidecar "0.5.19"]] ; for Cursive-integrated figwheel REPL
                       :plugins      [[lein-figwheel "0.5.19"]] ; for running `lein fighweel dev`
                       :source-paths ["dev/src"]
                       :repl-options {:init-ns user}}

             :uberjar {:main       sinostudy.handler
                       :aot        [sinostudy.handler]
                       :prep-tasks ["clean"
                                    "compile"
                                    ["cljsbuild" "once" "min"]]}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"]
                        :figwheel     {:on-jsload "sinostudy.core/mount-root"}
                        :compiler     {:main                 sinostudy.core
                                       :output-to            "resources/public/js/compiled/app.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :asset-path           "js/compiled/out"
                                       :source-map-timestamp true
                                       :optimizations        :none
                                       :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                                       :preloads             [devtools.preload
                                                              day8.re-frame-10x.preload]
                                       :external-config      {:devtools/config {:features-to-install :all}}}}

                       {:id           "min"
                        :source-paths ["src"]
                        :compiler     {:main            sinostudy.core
                                       :output-to       "resources/public/js/compiled/app.js"
                                       :optimizations   :advanced
                                       :closure-defines {goog.DEBUG false}
                                       :pretty-print    false}}]})
