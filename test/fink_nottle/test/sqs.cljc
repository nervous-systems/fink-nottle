(ns fink-nottle.test.sqs
  (:require [fink-nottle.internal]
            [fink-nottle.sqs :as sqs]
            [fink-nottle.test.sqs.util :refer [with-transient-queue!]]
            [glossop.core #? (:clj :refer :cljs :refer-macros) [go-catching <?]]
            [fink-nottle.test.common :refer [creds]]
            [fink-nottle.test.util
             #? (:clj :refer :cljs :refer-macros) [deftest is]]))

(defn ba->seq [x]
  #? (:clj
      (seq x)
      :cljs
      (for [i (range (aget x "length"))]
        (.readInt8 x i))))

(deftest binary-round-trip
  (let [input #? (:clj
                  (byte-array [0 0xFFF 97])
                  :cljs
                  (js/Buffer (clj->js [0 0xFFF 97])))]
    (with-transient-queue!
      (fn [{:keys [url]}]
        (go-catching
          (<? (sqs/send-message! creds url {:body "body" :attrs {:x input}}))
          (let [[{{output :x} :attrs :as g}]
                (<? (sqs/receive-message!
                     creds url {:wait-seconds 2 :attrs [:x]}))]
            (is (= (ba->seq input) (ba->seq output)))))))))

(deftest queue-size
  (with-transient-queue!
    (fn [{:keys [url]}]
      (go-catching
        (is (= 0 (<? (sqs/queue-size! creds url))))))))
