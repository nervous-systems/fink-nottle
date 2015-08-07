(ns fink-nottle.sqs
  (:require [fink-nottle.internal.sqs]
            [fink-nottle.internal :as internal]
            [#? (:clj clojure.core.async :cljs cljs.core.async) :as async]
            [glossop.core :as g
             #? (:clj :refer :cljs :refer-macros) [go-catching <?]]))

(internal/defissuers
  :sqs
  {change-message-visibility       [queue-url receipt-handle visibility-timeout]
   change-message-visibility-batch [queue-url messages]
   list-dead-letter-source-queues  [queue-url]

   create-queue         [queue-name]
   delete-message       [queue-url receipt-handle]
   delete-message-batch [queue-url messages]
   delete-queue         [queue-url]
   get-queue-attributes [queue-url attrs]
   set-queue-attributes [queue-url name value]

   get-queue-url      [queue-name]
   list-queues        []
   purge-queue        [queue-url]
   add-permission     [queue-url]
   remove-permission  [queue-url label]
   send-message       [queue-url]
   send-message-batch [queue-url messages]})

(defn get-queue-attribute! [creds q attr & [{:keys [chan close?]}]]
  (cond->
      (go-catching
        (-> (get-queue-attributes! creds q [attr]) <? (get attr)))
    chan (async/pipe chan close?)))
#? (:clj (def get-queue-attribute!! (comp g/<?! get-queue-attribute!)))

(def set-queue-attribute! set-queue-attributes!)
#? (:clj (def set-queue-attribute!! (comp g/<?! set-queue-attribute!)))

(defn queue-attribute-fetcher [attr]
  (fn [creds q & [req-opts]]
    (get-queue-attribute! creds q attr nil req-opts)))

(def queue-size!  (queue-attribute-fetcher :approximate-number-of-messages))
#? (:clj (def queue-size!! (comp g/<?! queue-size!)))

(def queue-arn!  (queue-attribute-fetcher :queue-arn))
#? (:clj (def queue-arn!! (comp g/<?! queue-arn!)))

(defn processed! [creds queue-url {:keys [receipt-handle]} & [req-opts]]
  (delete-message! creds queue-url receipt-handle req-opts))
#? (:clj (def processed!! (comp g/<?! processed!)))

(defn receive-message! [creds queue-url & [extra req-opts]]
  (eulalie.support/issue-request!
   (merge
    {:service :sqs
     :target :receive-message
     :creds creds
     :body (merge {:attrs :all :queue-url queue-url} extra)}
    req-opts)
   (partial internal/restructure-request :sqs)
   (partial internal/handle-response :sqs)))

#? (:clj (def receive-message!! (comp g/<?! receive-message!)))
