(ns fink-nottle.internal.sqs
  (:require [clojure.set :as set]
            [fink-nottle.internal :refer [restructure-request]]))

(defmethod restructure-request [:sqs :change-message-visibility] [_ _ m]
  (set/rename-keys m {:receipt :receipt-handle
                      :visibility :visibility-timeout}))
(defmethod restructure-request [:sqs :delete-message] [_ _ m]
  (set/rename-keys m {:receipt :receipt-handle}))
(defmethod restructure-request [:sqs :receive-messages] [_ _ m]
  (set/rename-keys m {:maximum :maximum-number-of-messages
                      :wait-seconds :wait-time-seconds}))

