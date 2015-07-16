(ns fink-nottle.sqs.tagged)

(defmulti  message-in (fn [tag body] tag))
(defmethod message-in :default [_ body] body)
(defmulti  message-out (fn [tag body] tag))
(defmethod message-in :default [_ body] body)
