(ns fink-nottle.sqs
  (:require [fink-nottle.internal :as i]
            [fink-nottle.internal.sqs]
            [glossop :refer [<?! <? go-catching]]
            [plumbing.core :refer [fn->]]))

(i/defissuers
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
   receive-message    [queue-url]
   add-permission     [queue-url]
   remove-permission  [queue-url label]
   send-message       [queue-url]
   send-message-batch [queue-url messages]})

(defn get-queue-attribute! [creds q attr]
  (go-catching
    (-> (get-queue-attributes! creds q [attr]) <? attr)))
(def get-queue-attribute!! (comp <?! get-queue-attribute!))

(def set-queue-attribute! set-queue-attributes!)
(def set-queue-attribute!! (comp <?! set-queue-attribute!))

(defn queue-attribute-fetcher [attr]
  (fn [creds q]
    (get-queue-attribute! creds q attr)))

(def queue-size!  (queue-attribute-fetcher :approximate-number-of-messages))
(def queue-size!! (comp <?! queue-size!))

(def queue-arn!  (queue-attribute-fetcher :queue-arn))
(def queue-arn!! (comp <?! queue-arn!))

(defn processed! [creds queue-url {:keys [receipt-handle]} & [extra]]
  (delete-message! creds queue-url receipt-handle extra))
(def processed!! (comp <?! processed!))
