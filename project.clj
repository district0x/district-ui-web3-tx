(defproject district0x/district-ui-web3-tx "1.0.11"
  :description "district UI module for handling web3 transactions"
  :url "https://github.com/district0x/district-ui-web3-tx"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[akiroz.re-frame/storage "0.1.2"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-ajax "0.8.0"]
                 [cljs-web3 "0.19.0-0-9"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [district0x.re-frame/web3-fx "1.0.5"]
                 [district0x/bignumber "1.0.1"]
                 [district0x/district-cljs-utils "1.0.4"]
                 [district0x/district-ui-web3 "1.0.1"]
                 [district0x/re-frame-interval-fx "1.0.2"]
                 [district0x/re-frame-spec-interceptors "1.0.1"]
                 [district0x/re-frame-window-fx "1.0.2"]
                 [mount "0.1.11"]
                 [org.clojure/clojurescript "1.9.946"]
                 [re-frame "0.10.2"]]

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :npm {:devDependencies [[karma "1.7.1"]
                          [karma-chrome-launcher "2.2.0"]
                          [karma-cli "1.0.1"]
                          [karma-cljs-test "0.1.0"]]}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [day8.re-frame/test "0.1.5"]
                                  [district0x/district-ui-smart-contracts "1.0.2"]
                                  [district0x/district-ui-web3-accounts "1.0.3"]
                                  [org.clojure/clojure "1.8.0"]
                                  [org.clojure/tools.nrepl "0.2.13"]]
                   :plugins [[lein-cljsbuild "1.1.7"]
                             [lein-doo "0.1.8"]
                             [lein-npm "0.6.2"]]}}

  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "tests-output/tests.js"
                                   :output-dir "tests-output"
                                   :main "tests.runner"
                                   :optimizations :none}}]})
