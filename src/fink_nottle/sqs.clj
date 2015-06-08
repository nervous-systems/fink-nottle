(ns fink-nottle.sqs
  (:require [fink-nottle.internal :as i] :reload
            [fink-nottle.internal.sqs] :reload))

(i/defissuers
  :sqs
  {change-message-visibility       [queue-url receipt-handle visibility-timeout]
   change-message-visibility-batch [queue-url messages]
   list-dead-letter-source-queues  [queue-url]

   create-queue         [queue-name attrs]
   delete-message       [queue-url receipt-handle]
   delete-message-batch [queue-url messages]
   delete-queue         [queue-url]
   get-queue-attributes [queue-url attrs]

   get-queue-url     [queue-name]
   list-queues       []
   purge-queue       [queue-url]
   receive-message   [queue-url]
   add-permission    [queue-url label aws-accounts actions]
   remove-permission [queue-url label]
   send-message      [queue-url message-body]})
