(ns fink-nottle.sqs.channeled-test
  (:require [fink-nottle.sqs.channeled :as channeled]
            [eulalie.sqs.test-util :as sqs-util]
            [fink-nottle.sqs :as sqs]
            [fink-nottle.test-util :refer [creds]]
            [clojure.test :refer :all]
            [clojure.core.async :as a]))

(deftest receive!+
  (sqs-util/with-transient-queue
    (fn [{:keys [url]}]
      (let [msgs (for [i (range 11)] {:body (str i)})]
        (sqs/send-message-batch!! creds url (take 10 msgs) {:generate-ids true})
        (sqs/send-message!! creds url (last msgs))
        (let [in-chan   (channeled/receive! creds url)
              in-bodies (->> in-chan (a/take 11) (a/into []) a/<!!
                             (map :body))]
          (a/close! in-chan)
          (is (= (into #{} (map :body msgs))
                 (into #{} in-bodies))))))))

(defn recording-batch-mock [chan]
  (fn [batch]
    (a/go
      (a/<! (a/onto-chan chan batch false))
      {})))

(def closed-ch (a/chan))
(a/close! closed-ch)

(deftest batching-channel+success
  (let [record-chan (a/chan 2)
        {:keys [error-chan]}
        (channeled/batching-channel*
         (recording-batch-mock record-chan)
         {:in-chan (a/to-chan [{:id "0"} {:id "1"}])
          :timeout-fn (constantly closed-ch)})]
    (a/alt!!
      (a/timeout 1000) (is false "Test timed out")
      (->> record-chan (a/take 2) (a/into []))
      ([messages] (is (= #{"0" "1"} (into #{} (map :id messages))))))))

(defn failing-batch-mock [batch]
  (a/go {:failed
         (into {}
           (for [{:keys [id]} batch]
             [id {:batch-id id :code :mysterious}]))}))

(deftest batching-channel+failure
  (let [{:keys [error-chan]}
        (channeled/batching-channel*
         failing-batch-mock
         {:in-chan (a/to-chan [{:id "0"} {:id "1"}])
          :timeout-fn (constantly closed-ch)})]
    (a/alt!!
      (a/timeout 1000) (is false "Test timed out")
      (a/into [] error-chan)
      ([failures]
       (is (= #{"0" "1"}
              (into #{} (for [f failures]
                          (-> f ex-data :batch-id)))))))))
