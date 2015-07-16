(ns fink-nottle.sns
  (:require [fink-nottle.internal.sns]
            #?@ (:clj
                 [[glossop.core :refer [<?!]]
                  [fink-nottle.internal :refer [defissuers]]]
                 :cljs
                 [[cljs.core.async]]))
  #? (:cljs (:require-macros [fink.nottle.internal :refer [defissuers]])))

(defissuers
  :sns
  {create-topic         [name]
   delete-topic         [topic-arn]
   get-topic-attributes [topic-arn]
   set-topic-attributes [topic-arn name value]
   list-topics          []

   confirm-subscription [topic-arn token]
   list-subscriptions   []
   list-subscriptions-by-topic [topic-arn]

   add-permission    [topic-arn]
   remove-permission [topic-arn label]

   delete-endpoint         [endpoint-arn]
   get-endpoint-attributes [endpoint-arn]

   publish     [message]
   subscribe   [topic-arn protocol endpoint]
   unsubscribe [subscription-arn]
   get-subscription-attributes [subscription-arn]
   set-subscription-attributes [subscription-arn name value]

   create-platform-application [platform name attrs]
   delete-platform-application [platform-application-arn]
   list-platform-applications  []
   get-platform-application-attributes    [platform-application-arn]
   set-platform-application-attributes    [platform-application-arn attrs]
   list-endpoints-by-platform-application [platform-application-arn]

   create-platform-endpoint [platform-application-arn token]})

(def set-subscription-attribute! set-subscription-attributes!)
#? (:clj (def set-subscription-attribute!!
           (comp <?! set-subscription-attribute!)))

(def set-topic-attribute! set-topic-attributes!)
#? (:clj (def set-topic-attribute!! (comp <?! set-topic-attribute!)))

(defn publish-topic! [creds topic-arn message & [extra]]
  (publish! creds message (assoc extra :topic-arn topic-arn)))
#? (:clj (def publish-topic!! (comp <?! publish-topic!)))

(defn publish-endpoint! [creds target-arn message & [extra]]
  (publish! creds message (assoc extra :target-arn target-arn)))
#? (:clj (def publish-endpoint!! (comp <?! publish-endpoint!)))
