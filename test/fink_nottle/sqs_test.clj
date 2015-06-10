(ns fink-nottle.sqs-test
  (:require [fink-nottle.internal]
            [fink-nottle.sqs :as sqs]
            [eulalie.sqs.test-util :as sqs-util]
            [fink-nottle.test-util :refer [creds]]
            [clojure.test :refer :all]))

(deftest binary-round-trip+
  (let [input (byte-array [0 0xFFF 97])]
    (sqs-util/with-transient-queue
      (fn [{:keys [url]}]
        (sqs/send-message!! creds url "body" {:attrs {:x input}})
        (let [[{{output :x} :attrs}]
              (sqs/receive-message!!
               creds url {:wait-seconds 2 :attrs [:x]})]
          (is (= (map identity input)
                 (map identity output))))))))

(deftest queue-size+
  (sqs-util/with-transient-queue
    (fn [{:keys [url]}]
      (is (= 0 (sqs/queue-size!! creds url))))))


