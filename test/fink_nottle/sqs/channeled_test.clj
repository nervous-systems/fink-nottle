(ns fink-nottle.sqs.channeled-test
  (:require [fink-nottle.sqs.channeled :as channeled]
            [fink-nottle.sqs :as sqs]
            [fink-nottle.test-util :refer :all]
            [clojure.test :refer :all]
            [clojure.core.async :as async]))

(deftest receive-message+
  (let [url  (sqs/create-queue!! creds (random-name))
        msgs (for [i (range 11)] {:body (str i)})]
    (try
      (sqs/send-message-batch!!
       creds url (take 10 msgs) {:generate-ids true})
      (sqs/send-message!! creds url (:body (last msgs)))
      (let [out-bodies (->> (channeled/receive-message! creds url)
                            (async/take 11)
                            (async/into [])
                            async/<!!
                            (map :body))]
        (is (= (into #{} (map :body msgs))
               (into #{} out-bodies))))
      (finally
        (sqs/delete-queue! creds url)))))

