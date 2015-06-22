(defproject io.nervous/fink-nottle "0.2.0"
  :description "Asynchronous Clojure client for the Amazon SNS service"
  :url "https://github.com/nervous-systems/fink-nottle"
  :license {:name "Unlicense" :url "http://unlicense.org/UNLICENSE"}
  :scm {:name "git" :url "https://github.com/nervous-systems/fink-nottle"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :signing {:gpg-key "moe@nervous.io"}
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src" "test"]
  :dependencies [[org.clojure/clojure    "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.codec "0.1.0"]

                 [io.nervous/eulalie     "0.5.0-SNAPSHOT"]
                 [io.nervous/glossop     "0.1.0"]

                 [prismatic/plumbing     "0.4.1"]
                 [cheshire               "5.5.0"]]
  :exclusions [[org.clojure/clojure]])
