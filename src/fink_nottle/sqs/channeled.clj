(ns fink-nottle.sqs.channeled
  (:require [fink-nottle.sqs :as sqs]
            [fink-nottle.internal.util :as util]
            [clojure.core.async :as a]
            [glossop :as g :refer [<? go-catching]]))

(defn receive-message! [creds queue-url & [{:keys [chan] :as params}]]
  (let [{:keys [maximum] :as params} (merge {:maximum 10 :wait-seconds 20} params)
        chan   (or chan (a/chan maximum))]
    (go-catching
      (loop []
        (let [messages
              (-> (sqs/receive-message! creds queue-url params) <? not-empty)]
          (if (or (not messages) (<? (util/onto-chan? chan messages)))
            (recur)
            (a/close! chan)))))
    chan))

(defn- read-timeout! [timeout-ms chan & [{:keys [timeout-fn]}]]
  )

(defn- failure->throwable [{:keys [code] :as failure}]
  (ex-info (name code) (assoc failure :type code)))

(defn- batch-send! [creds queue-url batch error-chan]
  (a/go
    (try
      (let [{:keys [failed]} (<? (sqs/send-message-batch! creds queue-url batch))]
        (when-let [exs (some->> failed vals (map failure->throwable) not-empty)]
          (<? (a/onto-chan error-chan exs false))))
      (catch Exception e
        (a/>! error-chan e)))))

(defn- batch-cleanup! [creds queue-url batch error-chan]
  (go-catching
    (when (not-empty batch)
      (<? (batch-send! creds queue-url batch error-chan)))
    (a/close! error-chan)))

(defn send-message-batch!
  [creds queue-url & [{:keys [period-ms threshold in-chan error-chan timeout-fn]
                       :or {period-ms 200 threshold 10 timeout-fn a/timeout}}]]
  (let [in-chan    (or in-chan (a/chan))
        error-chan (or error-chan (a/chan))]
    (go-catching
      (loop [batch []]
        (let [msg (if (not-empty batch)
                    (a/alt!
                      (timeout-fn period-ms) ::timeout
                      in-chan ([v] v))
                    (a/<! in-chan))]

          (if (nil? msg)
            (<? (batch-cleanup! creds queue-url batch error-chan))
            (let [batch (cond-> batch (not= msg ::timeout) (conj msg))]
              (if (or (= threshold (count batch)) (= msg ::timeout))
                (do (<? (batch-send! creds queue-url batch error-chan))
                    (recur []))
                (recur batch)))))))
    {:in-chan in-chan :error-chan error-chan}))
