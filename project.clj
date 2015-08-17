(defproject io.nervous/fink-nottle "0.4.3"
  :description "Asynchronous Clojure/Clojurescript client for the Amazon SNS & SQS services"
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

                 [io.nervous/eulalie     "0.6.1"]

                 [prismatic/plumbing     "0.4.1"]
                 [cheshire               "5.5.0"]
                 [base64-clj             "0.1.1"]]
  :exclusions [[org.clojure/clojure]]
  :node-dependencies [[source-map-support "0.2.8"]
                      [bignumber.js "2.0.7"]]
  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-npm "0.5.0"]]
  :cljsbuild
  {:builds [{:id "main"
             :source-paths ["src"]
             :compiler {:output-to "fink-nottle.js"
                        :target :nodejs
                        :hashbang false
                        :optimizations :none
                        :source-map true}}
            {:id "test-none"
             :source-paths ["src" "test"]
             :notify-command ["node" "target/test-none/fink-nottle-test.js"]
             :compiler {:output-to "target/test-none/fink-nottle-test.js"
                        :output-dir "target/test-none"
                        :target :nodejs
                        :optimizations :none
                        :main "fink-nottle.test.runner"}}
            {:id "test-advanced"
             :source-paths ["src" "test"]
             :notify-command ["node" "target/test-advanced/fink-nottle-test.js"]
             :compiler {:output-to "target/test-advanced/fink-nottle-test.js"
                        :output-dir "target/test-advanced"
                        :target :nodejs
                        :optimizations :advanced}}]}
  :profiles {:dev
             {:repl-options
              {:nrepl-middleware
               [cemerick.piggieback/wrap-cljs-repl]}
              :node-dependencies []
              :dependencies
              [[com.cemerick/piggieback "0.2.1"]
               [org.clojure/tools.nrepl "0.2.10"]]
              :source-paths ["src" "test"]}})
