(ns fink-nottle.sns.channeled
  (:require [fink-nottle.sns :as sns]
            [glossop.util :refer [onto-chan?]]
            #?@ (:clj
                 [[glossop.core :refer [<? go-catching]]
                  [clojure.core.async :as async]]
                 :cljs
                 [[cljs.core.async :as async]]))
  #? (:cljs (:require-macros [glossop.macros :refer [<? go-catching]])))

(defn paginate! [f map-arg {:keys [limit maximum chan]}]
  (assert (or limit chan)
          "Please supply either a page-size (limit) or output channel")
  (let [chan (or chan (async/chan limit))]
    (go-catching
      (try
        (loop [next-token nil n 0]
          (let [items (<? (f (cond-> map-arg next-token
                                     (assoc :next-token next-token))))
                n (+ n (count items))
                {:keys [next-token]} (meta items)]
            (if (and (<? (onto-chan? chan items))
                     next-token
                     (or (not maximum) (< n maximum)))
              (recur next-token n)
              (async/close! chan))))
        (catch #? (:clj Exception :cljs js/Error) e
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
