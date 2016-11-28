(defproject io.nervous/fink-nottle "0.4.7"
  :description "Asynchronous Clojure/Clojurescript client for the Amazon SNS & SQS services"
  :url "https://github.com/nervous-systems/fink-nottle"
  :license {:name "Unlicense" :url "http://unlicense.org/UNLICENSE"}
  :scm {:name "git" :url "https://github.com/nervous-systems/fink-nottle"}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure    "1.8.0"]
                 [io.nervous/eulalie     "0.6.10"]
                 [cheshire               "5.6.3"]
                 [base64-clj             "0.1.1"]]
  :npm {:dependencies [[source-map-support "0.2.8"]
                       [bignumber.js       "2.4.0"]]}
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-npm       "0.6.2"]]
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
  :profiles {:dev {:source-paths ["src" "test"]}})
