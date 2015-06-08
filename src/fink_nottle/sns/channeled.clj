(ns fink-nottle.sns.channeled
  (:require [fink-nottle.sns :as sns]
            [fink-nottle.internal.util :as util]
            [glossop :as g :refer [<?]]
            [clojure.core.async :as async]))

(defn paginate! [f map-arg {:keys [limit maximum chan]}]
  (assert (or limit chan)
          "Please supply either a page-size (limit) or output channel")
  (let [chan (or chan (async/chan limit))]
    (g/go-catching
      (try
        (loop [next-token nil n 0]
          (let [items (<? (f (cond-> map-arg next-token
                                     (assoc :next-token next-token))))
                n (+ n (count items))
                {:keys [next-token]} (meta items)]
            (if (and (<? (util/onto-chan? chan items))
                     next-token
                     (or (not maximum) (< n maximum)))
              (recur next-token n)
              (async/close! chan))))
        (catch Exception e
          (async/>! chan e)
          (async/close! chan))))
    chan))

(defn list-platform-applications! [creds & [opts]]
  (paginate!
   (partial sns/list-platform-applications! creds)
   nil (merge {:limit 100} opts)))

(defn list-endpoints-by-platform-application! [creds arn & [opts]]
  (paginate!
   (partial sns/list-endpoints-by-platform-application! creds arn)
   nil (merge {:limit 100} opts)))

(defn list-subscriptions! [creds & [opts]]
  (paginate!
   (partial sns/list-subscriptions! creds)
   nil (merge {:limit 100} opts)))

(defn list-subscriptions-by-topic! [creds arn & [opts]]
  (paginate!
   (partial sns/list-subscriptions-by-topic! creds arn)
   nil (merge {:limit 100} opts)))
