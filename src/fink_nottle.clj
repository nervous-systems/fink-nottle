(ns fink-nottle
  (:require [fink-nottle.internal :as i]))

(i/defissuers
  {create-topic         [name]
   get-topic-attributes [topic-arn]

   delete-endpoint         [endpoint-arn]
   get-endpoint-attributes [endpoint-arn]
   delete-topic    [topic-arn]
   publish         [message]
   subscribe       [topic-arn protocol endpoint]
   create-platform-application [platform name attrs]
   create-platform-endpoint    [platform-application-arn token]
   list-platform-applications  []})
