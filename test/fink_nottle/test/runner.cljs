(ns fink-nottle.test.runner
  (:require [cljs.test]
            [fink-nottle.test.sqs]
            [fink-nottle.test.sqs.channeled]))

(defn run []
  (cljs.test/run-tests
   'fink-nottle.test.sqs
   'fink-nottle.test.sqs.channeled))

(enable-console-print!)

(set! *main-cli-fn* run)
