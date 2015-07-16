(ns fink-nottle.test.sqs.channeled
  (:require [fink-nottle.sqs.channeled :as channeled]
            [fink-nottle.test.sqs.util :as sqs.util :refer [with-transient-queue!]]
            [fink-nottle.sqs :as sqs]
            [fink-nottle.test.util :refer [creds]]
            #?@ (:clj
                 [[fink-nottle.test.async :refer [deftest]]
                  [clojure.core.async :as a :refer [alt!]]
                  [clojure.test :refer [is]]
                  [glossop.core :refer [<? go-catching]]]
                 :cljs
                 [[cemerick.cljs.test]
                  [cljs.core.async :as a]]))
  #? (:cljs (:require-macros [glossop.macros :refer [go-catching <?]]
                             [fink-nottle.test.async.macros :refer [deftest]]
                             [cljs.core.async.macros :refer [alt!]]
                             [cemerick.cljs.test :refer [is]])))

(deftest receive!
  (with-transient-queue!
    (fn [{:keys [url]}]
      (go-catching
        (let [msgs (for [i (range 11)] {:body (str i)})]
          (<? (sqs/send-message-batch! creds url (take 10 msgs) {:generate-ids true}))
          (<? (sqs/send-message! creds url (last msgs)))
          (let [in-chan   (channeled/receive! creds url)
                in-bodies (->> in-chan (a/take 11) (a/into []) <?
                               (map :body))]
            (a/close! in-chan)
            (is (= (into #{} (map :body msgs))
                   (into #{} in-bodies)))))))))

(defn recording-batch-mock [chan]
  (fn [batch]
    (go-catching
      (<? (a/onto-chan chan batch false))
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
    (go-catching
      (alt!
        (a/timeout 1000) (is false "Test timed out")
        (->> record-chan (a/take 2) (a/into []))
        ([messages] (is (= #{"0" "1"} (into #{} (map :id messages)))))))))

(defn failing-batch-mock [batch]
  (go-catching
    {:failed
     (into {}
       (for [{:keys [id]} batch]
         [id {:batch-id id :code :mysterious}]))}))

(deftest batching-channel+failure
  (let [{:keys [error-chan]}
        (channeled/batching-channel*
         failing-batch-mock
         {:in-chan (a/to-chan [{:id "0"} {:id "1"}])
          :timeout-fn (constantly closed-ch)})]
    (go-catching
      (alt!
        (a/timeout 1000) (is false "Test timed out")
        (a/into [] error-chan)
        ([failures]
         (is (= #{"0" "1"}
                (into #{} (for [f failures]
                            (-> f ex-data :batch-id))))))))))
