(ns fink-nottle.sqs.channeled
  (:require [fink-nottle.sqs :as sqs]
            [fink-nottle.internal.util :as util]
            [glossop.core #? (:clj :refer :cljs :refer-macros) [go-catching <?]]
            #?@ (:clj
                 [[clojure.core.async :as a :refer [>! alt!]]
                  [clojure.core.async.impl.protocols :as async-protocols]]
                 :cljs
                 [[cljs.core.async :as a :refer [>!]]
                  [cljs.core.async.impl.protocols :as async-protocols]]))
  #? (:cljs (:require-macros [cljs.core.async.macros :refer [alt!]])))

(defn receive! [creds queue-url & [params {:keys [chan close?] :or {close? true}}]]
  (let [{:keys [maximum] :as params} (merge {:maximum 10 :wait-seconds 20} params)
        chan (or chan (a/chan maximum))]
    (go-catching
      (try
        (loop []
          (let [messages (<? (sqs/receive-message! creds queue-url params))]
            (when-not (empty? messages)
              (<? (a/onto-chan chan messages false)))
            (when-not (async-protocols/closed? chan)
              (recur))))
        (catch #? (:clj Exception :cljs js/Error) e
          (>! chan e)))
      (when close?
        (a/close! chan)))
    chan))

(defn identify-batch [messages]
  (map-indexed
   (fn [i {:keys [id] :as m}]
     (cond-> m (not id) (assoc :id (str i))))
   messages))

(defn- failure->throwable [{:keys [code] :as failure}]
  (ex-info (name code) (assoc failure :type code)))

(defn- batch-send! [issue-fn batch error-chan]
  (go-catching
    (try
      (let [{:keys [failed]} (<? (issue-fn (identify-batch batch)))]
        (when-let [exs (some->> failed vals (map failure->throwable) not-empty)]
          (<? (a/onto-chan error-chan exs false))))
      (catch #? (:clj Exception :cljs js/Error) e
        (>! error-chan e)))))

(defn- batch-cleanup! [issue-fn batch error-chan close?]
  (go-catching
    (when (not-empty batch)
      (<? (batch-send! issue-fn batch error-chan)))
    (when close?
      (a/close! error-chan))))

(defn batching-channel*
  [issue-fn
   & [{:keys [period-ms threshold in-chan error-chan timeout-fn close?]
       :or {period-ms 200 threshold 10 timeout-fn a/timeout close? true}}]]
  (let [in-chan    (or in-chan (a/chan))
        error-chan (or error-chan (a/chan))]
    (go-catching
      (loop [batch []]
        (let [msg (if (not-empty batch)
                    (alt!
                      (timeout-fn period-ms) ::timeout
                      in-chan ([v] v))
                    (<? in-chan))]
          (if (nil? msg)
            (<? (batch-cleanup! issue-fn batch error-chan close?))
            (let [batch (cond-> batch (not= msg ::timeout) (conj msg))]
              (if (or (= threshold (count batch)) (= msg ::timeout))
                (do (<? (batch-send! issue-fn batch error-chan))
                    (recur []))
                (recur batch)))))))
    {:in-chan in-chan :error-chan error-chan}))

(defn batching-sends [creds queue-url & [opts]]
  (batching-channel*
   (partial sqs/send-message-batch! creds queue-url)
   opts))
(defn batching-deletes [creds queue-url & [opts]]
  (batching-channel*
   (partial sqs/delete-message-batch! creds queue-url)
   opts))
