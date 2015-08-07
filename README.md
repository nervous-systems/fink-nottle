# fink-nottle [![Build Status](https://travis-ci.org/nervous-systems/fink-nottle.svg?branch=master)](https://travis-ci.org/nervous-systems/fink-nottle)

[![Clojars Project](http://clojars.org/io.nervous/fink-nottle/latest-version.svg)](http://clojars.org/io.nervous/fink-nottle)

Asynchronous Clojure/Clojurescript client for Amazon's SQS & SNS services

 - [core.async](https://github.com/clojure/core.async)-based API
 - Targets both Clojure and Clojurescript/Node
 - Flexible, uniform interface across SNS & SQS
 - Channel-based batching of outgoing messages

## Documentation

The [API
introduction](https://github.com/nervous-systems/fink-nottle/wiki/)
on the wiki is a good place to start.

### Writing

* [Queuing on EC2 with core.async](https://nervous.io/clojure/aws/async/sqs/messaging/2015/06/15/fink-nottle-sqs/) (SQS)
* [Push Messaging on EC2 with core.async](https://nervous.io/clojure/aws/async/sns/messaging/2015/06/15/fink-nottle-sns/) (SNS)
* [Pushing Events Over Websockets with SNS & Elastic Beanstalk](https://nervous.io/clojure/async/sns/eb/docker/2015/06/22/sns-beanstalk-chat/) (SNS)

## SQS Example

```clojure
(require '[fink-nottle.sqs.tagged :as sqs.tagged]
         '[fink-nottle.sqs.channeled :as sqs.channeled])

(defmethod sqs.tagged/message-in  :edn [_ body]
  (clojure.edn/read-string body))
(defmethod sqs.tagged/message-out :edn [_ body] (pr-str body))

(defn send-loop! [creds queue-url]
  (let [{:keys [in-chan]}
        (sqs.channeled/batching-sends creds queue-url)]
    (go
      (loop [i 0]
        (>! in-chan {:body {:event :increment :value i}
                     :fink-nottle/tag :edn})
        (<! (async/timeout (rand-int 300)))
        (recur (inc i))))))

(defn receive-loop! [id creds queue-url]
  (let [messages (sqs.channeled/receive! creds queue-url)
        {deletes :in-chan} (sqs.channeled/batching-deletes creds queue-url)]
    (async/pipe messages deletes)))
```

# Clojurescript

All of the functionality (barring the synchronous convenience functions, and [sns.consume](https://github.com/nervous-systems/fink-nottle/wiki/sns.consume)) is
exposed via Clojurescript.  The implementation specifically targets
[Node](https://nodejs.org/), and uses
[lein-npm](https://github.com/RyanMcG/lein-npm) for declaring its dependency on
[bignumber.js](https://github.com/MikeMcl/bignumber.js/).

The specific use-case I had in mind for Node support is [writing AWS Lambda
functions in
Clojurescript](https://nervous.io/clojure/clojurescript/aws/lambda/node/lein/2015/07/05/lambda/).

See the [Eulalie
README](https://github.com/nervous-systems/eulalie#clojurescript) for other
Node-relevant details.


