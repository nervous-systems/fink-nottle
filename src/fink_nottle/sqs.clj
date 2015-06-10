(ns fink-nottle.sqs
  (:require [fink-nottle.internal :as i]
            [fink-nottle.internal.sqs]
            [glossop :refer [<?! <? go-catching]]))

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
   set-queue-attributes [queue-url attrs]

   get-queue-url      [queue-name]
   list-queues        []
   purge-queue        [queue-url]
   receive-message    [queue-url]
   add-permission     [queue-url label aws-accounts actions]
   remove-permission  [queue-url label]
   send-message       [queue-url message-body]
   send-message-batch [queue-url messages]})

(defn queue-attribute-fetcher [attr]
  (fn [creds q]
    (go-catching
      (-> (get-queue-attributes! creds q [attr]) <? attr))))

(def queue-size!  (queue-attribute-fetcher :approximate-number-of-messages))
(def queue-size!! (comp <?! queue-size!))

(def queue-arn!  (queue-attribute-fetcher :queue-arn))
(def queue-arn!! (comp <?! queue-arn!))
