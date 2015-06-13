(ns fink-nottle.sqs.channeled-test
  (:require [fink-nottle.sqs.channeled :as channeled]
            [eulalie.sqs.test-util :as sqs-util]
            [fink-nottle.sqs :as sqs]
            [fink-nottle.test-util :refer [creds]]
            [clojure.test :refer :all]
            [clojure.core.async :as a]))

(deftest receive-message+
  (sqs-util/with-transient-queue
    (fn [{:keys [url]}]
      (let [msgs (for [i (range 11)] {:body (str i)})]
        (sqs/send-message-batch!! creds url (take 10 msgs) {:generate-ids true})
        (sqs/send-message!! creds url (last msgs))
        (let [out-chan   (channeled/receive-message! creds url)
              out-bodies (->> out-chan (a/take 11) (a/into []) a/<!!
                              (map :body))]
          (a/close! out-chan)
          (is (= (into #{} (map :body msgs))
                 (into #{} out-bodies))))))))

(defn recording-batch-mock [chan]
  (fn [_ _ batch & _]
    (a/go
      (a/<! (a/onto-chan chan batch false))
      {})))

(def closed-ch (a/chan))
(a/close! closed-ch)

(deftest send-message-batch+sends
  (sqs-util/with-transient-queue
    (fn [{:keys [url]}]
      (let [record-chan (a/chan 2)]
        (with-redefs [sqs/send-message-batch! (recording-batch-mock record-chan)]
          (channeled/send-message-batch!
           creds url {:in-chan (a/to-chan [{:id "0"} {:id "1"}])
                      :timeout-fn (constantly closed-ch)})
          (a/alt!!
            (a/timeout 1000) (is false "Test timed out")
            (->> record-chan (a/take 2) (a/into []))
            ([sends] (is (= #{"0" "1"} (into #{} (map :id sends)))))))))))

(defn failing-batch-mock [_ _ batch & _]
  (a/go {:failed
         (into {}
           (for [{:keys [id]} batch]
             [id {:batch-id id :code :mysterious}]))}))

(deftest send-message-batch+failure
  (sqs-util/with-transient-queue
    (fn [{:keys [url]}]
      (with-redefs [sqs/send-message-batch! failing-batch-mock]
        (let [{:keys [error-chan]}
              (channeled/send-message-batch!
               creds url {:in-chan (a/to-chan [{:id "0"} {:id "1"}])
                          :timeout-fn (constantly closed-ch)})]
          (a/alt!!
            (a/timeout 1000) (is false "Test timed out")
            (a/into [] error-chan)
            ([failures]
             (is (= #{"0" "1"}
                    (into #{} (for [f failures]
                                (-> f ex-data :batch-id))))))))))))
