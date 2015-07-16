(ns fink-nottle.sqs
  (:require [fink-nottle.internal.sqs]
            [fink-nottle.internal :as i #?@ (:clj [:refer [defissuers]])]
            [eulalie.support]
            #? (:clj
                [glossop.core :refer [<?! <? go-catching]]
                :cljs
                [cljs.core.async]))
  #? (:cljs (:require-macros [fink.nottle.internal :refer [defissuers]]
                             [glossop.macros :refer [<? go-catching]])))

(defissuers
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

(defn get-queue-attribute! [creds q attr]
  (go-catching
    (-> (get-queue-attributes! creds q [attr]) <? attr)))
#? (:clj (def get-queue-attribute!! (comp <?! get-queue-attribute!)))

(def set-queue-attribute! set-queue-attributes!)
#? (:clj (def set-queue-attribute!! (comp <?! set-queue-attribute!)))

(defn queue-attribute-fetcher [attr]
  (fn [creds q]
    (get-queue-attribute! creds q attr)))

(def queue-size!  (queue-attribute-fetcher :approximate-number-of-messages))
#? (:clj (def queue-size!! (comp <?! queue-size!)))

(def queue-arn!  (queue-attribute-fetcher :queue-arn))
#? (:clj (def queue-arn!! (comp <?! queue-arn!)))

(defn processed! [creds queue-url {:keys [receipt-handle]} & [extra]]
  (delete-message! creds queue-url receipt-handle extra))
#? (:clj (def processed!! (comp <?! processed!)))

(defn receive-message! [creds queue-url & [extra]]
  (go-catching
    (let [resp
          (<? (eulalie.support/issue-request!
               {:service :sqs
                :target :receive-message
                :creds creds
                :body (i/restructure-request
                       :sqs
                       :receive-message
                       (merge {:attrs :all :queue-url queue-url} extra))}))]
      (i/handle-response :sqs :receive-message resp))))
#? (:clj (def receive-message!! (comp <?! receive-message!)))
