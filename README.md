# fink-nottle [![Build Status](https://travis-ci.org/nervous-systems/fink-nottle.svg?branch=master)](https://travis-ci.org/nervous-systems/fink-nottle)

[![Clojars Project](http://clojars.org/io.nervous/fink-nottle/latest-version.svg)](http://clojars.org/io.nervous/fink-nottle)

Asynchronous Clojure client for Amazon's SQS & SNS services

## SQS Example

```clojure
(defmethod sqs.tagged/message-in  :edn [_ body]
  (clojure.edn/read-string body))
(defmethod sqs.tagged/message-out :edn [_ body] (pr-str body))

(defn send-loop! [creds queue-url]
  (let [{:keys [in-chan]}
        (channeled/batching-sends creds queue-url)]
    (go
      (loop [i 0]
        (>! in-chan {:body {:event :increment :value i}
                     :fink-nottle/tag :edn})
        (<! (async/timeout (rand-int 300)))
        (recur (inc i))))))

(defn receive-loop! [id creds queue-url]
  (let [messages (channeled/receive! creds queue-url)
        deletes  (channeled/batching-deletes creds queue-url)]
    (go
      (loop []
        (let [{:keys [body attrs] :as message} (<! messages)]
          (>! deletes message)
          (recur))))))
```
