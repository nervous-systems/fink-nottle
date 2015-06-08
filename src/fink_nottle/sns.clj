(ns fink-nottle.sns
  (:require [fink-nottle.internal :as i] :reload
            [fink-nottle.internal.sns] :reload))

(i/defissuers
  :sns
  {create-topic         [name]
   delete-topic         [topic-arn]
   get-topic-attributes [topic-arn]
   set-topic-attributes [topic-arn attribute-name]
   list-topics          []

   confirm-subscription [topic-arn token]
   list-subscriptions   []
   list-subscriptions-by-topic [topic-arn]

   add-permission    [topic-arn label aws-accounts actions]
   remove-permission [topic-arn label]

   delete-endpoint         [endpoint-arn]
   get-endpoint-attributes [endpoint-arn]

   publish     [message]
   subscribe   [topic-arn protocol endpoint]
   unsubscribe [subscription-arn]
   get-subscription-attributes [subscription-arn]
   set-subscription-attributes [subscription-arn attribute-name]

   create-platform-application [platform name attrs]
   delete-platform-application [platform-application-arn]
   list-platform-applications  []
   get-platform-application-attributes    [platform-application-arn]
   set-platform-application-attributes    [platform-application-arn attrs]
   list-endpoints-by-platform-application [platform-application-arn]

   create-platform-endpoint [platform-application-arn token]})

