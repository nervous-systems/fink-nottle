(defproject io.nervous/fink-nottle "1.0.0-SNAPSHOT"
  :description "Asynchronous Clojure client for the Amazon SNS service"
  :url "https://github.com/nervous-systems/fink-nottle"
  :license {:name "Unlicense" :url "http://unlicense.org/UNLICENSE"}
  :scm {:name "git" :url "https://github.com/nervous-systems/fink-nottle"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :signing {:gpg-key "moe@nervous.io"}
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src" "test"]
  :dependencies [[org.clojure/clojure        "1.7.0"]
                 [org.clojure/core.async     "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript  "0.0-3308"]

                 [io.nervous/eulalie     "1.0.0-SNAPSHOT"]
                 [io.nervous/glossop     "1.0.0-SNAPSHOT"]

                 [prismatic/plumbing     "0.4.1"]
                 [cheshire               "5.5.0"]
                 [base64-clj             "0.1.1"]]
  :exclusions [[org.clojure/clojure]]
  :node-dependencies [[source-map-support "0.2.8"]
                      [bignumber.js "2.0.7"]]
  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-npm "0.5.0"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :cljsbuild
  {:builds [{:id "main"
             :source-paths ["src"]
             :compiler {:output-to "fink-nottle.js"
                        :target :nodejs
                        :hashbang false
                        :optimizations :none
                        :source-map true}}
            {:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "target/js-test/test.js"
                        :output-dir "target/js-test"
                        :target :nodejs
                        :hashbang false
                        :source-map true
                        :optimizations :none}}]
   :test-commands {"node" ["node" "runner-none.js" "target/js-test"
                           "target/js-test/test.js"]}}
  :profiles {:dev
             {:repl-options
              {:nrepl-middleware
               [cemerick.piggieback/wrap-cljs-repl]}
              :node-dependencies []
              :dependencies
              [[com.cemerick/piggieback "0.2.1"]
               [org.clojure/tools.nrepl "0.2.10"]
               [com.cemerick/clojurescript.test "0.3.3"]]
              :source-paths ["src" "test"]}})
