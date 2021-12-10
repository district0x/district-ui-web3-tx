(defproject district0x/district-ui-web3-tx "1.0.13-SNAPSHOT"
  :description "district UI module for handling web3 transactions"
  :url "https://github.com/district0x/district-ui-web3-tx"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[akiroz.re-frame/storage "0.1.4"]
                 [camel-snake-kebab "0.4.1"]
                 [cljs-ajax "0.8.0"]
                 [cljs-web3 "0.19.0-0-11"]
                 [day8.re-frame/forward-events-fx "0.0.6"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [district0x.re-frame/web3-fx "1.0.5"]
                 [district0x/bignumber "1.0.3"]
                 [district0x/district-cljs-utils "1.0.4"]
                 [district0x/district-ui-web3 "1.3.2"]
                 [district0x/eip55 "0.0.1"]
                 [district0x/re-frame-interval-fx "1.0.2"]
                 [district0x/re-frame-spec-interceptors "1.0.1"]
                 [district0x/re-frame-window-fx "1.1.0"]
                 [mount "0.1.16"]
                 [org.clojure/clojurescript "1.10.597"]
                 [re-frame "0.11.0"]]

  :doo {:karma {:config {"colors" true
                         "files" [{"pattern" "tests-output/*.js" "watched" true "served" true "included" true }
                                  {"pattern" "tests-output/**/*.js" "watched" true "served" true "included" true}
                                  {"pattern" "resources/public/contracts/build/*.json" "watched" false "served" true "included" false "nocache" true}]}}
        :paths {:karma "./node_modules/karma/bin/karma"}}

  :profiles {:dev {:source-paths ["src" "test"]
                   :resource-paths ["resources"]
                   :dependencies [[day8.re-frame/test "0.1.5"]
                                  [district0x/district-ui-smart-contracts "1.0.8"]
                                  [district0x/district-ui-web3-accounts "1.0.7"]
                                  [org.clojure/clojure "1.10.1"]
                                  [lein-doo "0.1.11"]]
                   :plugins [[lein-cljsbuild "1.1.7"]
                             [lein-doo "0.1.11"]]}}

  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]
                        ["releases"  {:url "https://clojars.org/repo"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["deploy"]]

  :cljsbuild {:builds [{:id "browser-tests"
                        :source-paths ["src" "test"]
                        :resource-paths ["resources"]
                        :compiler {:output-to "tests-output/tests.js"
                                   :output-dir "tests-output"
                                   :main "tests.runner"
                                   :optimizations :none}}]})
