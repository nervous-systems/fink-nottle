(ns fink-nottle.internal.sqs
  (:require [clojure.set :as set]
            [eulalie.sqs]
            [fink-nottle.internal :as i]
            [fink-nottle.internal.platform :refer [->int] :as platform]
            [fink-nottle.internal.util :as util]
            [fink-nottle.sqs.tagged :as tagged]
            [plumbing.core :refer [map-vals]]))

(def key->xform
  {:approximate-number-of-messages ->int
   :approximate-number-of-messages-not-visible ->int
   :approximate-number-of-messages-delayed ->int
   :approximate-receive-count ->int
   :approximate-first-receive-timestamp ->int

   :visibility-timeout ->int
   :created-timestamp ->int
   :last-modified-timestamp ->int
   :maximum-message-size ->int
   :maximum-retention-period ->int
   :delay-seconds ->int
   :receive-message-wait-time-seconds ->int
   :sent-timestamp ->int
   :sender-fault util/->bool})

(defn attr-val-out [x]
  (let [x (cond-> x (keyword? x) name)]
   (cond
     (string? x)              [:string x]
     (number? x)              [:number (str x)]
     (platform/byte-array? x) [:binary (platform/ba->b64-string x)]
     :else (throw (ex-info "bad-attr-type"
                           {:type :bad-attr-type :value x})))))

(defn attr-val-in [[tag value]]
  (case tag
    :string value
    :number (platform/string->number value)
    :binary (platform/b64-string->ba value)))

(defn augment-outgoing [{:keys [attrs body fink-nottle/tag] :as m}]
  (cond-> m tag
          (assoc
           :body (tagged/message-out tag body)
           :attrs (assoc attrs :fink-nottle-tag tag))))

(defmethod i/restructure-request [:sqs :get-queue-url] [_ _ m]
  (set/rename-keys m {:account :queue-owner-aws-account-id}))

(defmethod i/restructure-request [:sqs :send-message]
  [_ _ message]
  (let [{:keys [attrs] :as message} (augment-outgoing message)]
    (assoc message :attrs (map-vals attr-val-out attrs))))

(defmethod i/restructure-request [:sqs :send-message-batch]
  [_ _ {:keys [messages generate-ids] :as m}]
  (assoc m :messages
         (map-indexed
          (fn [i msg]
            (let [{:keys [attrs] :as msg} (augment-outgoing msg)]
              (cond-> (assoc msg :attrs (map-vals attr-val-out attrs))
                generate-ids (assoc :id (str i)))))
          messages)))

(defn augment-incoming [{{:keys [fink-nottle-tag]} :attrs body :body :as m}]
  (cond-> m fink-nottle-tag
          (assoc :body (tagged/message-in (keyword fink-nottle-tag) body))))

(defn restructure-message [{:keys [attrs] :as m}]
  (-> m
      (util/walk-values key->xform)
      (assoc :attrs (map-vals attr-val-in attrs))
      augment-incoming))

(defmethod i/restructure-response [:sqs :receive-message] [_ _ ms]
  (map restructure-message ms))

(defmethod i/restructure-response [:sqs :get-queue-attributes] [_ _ attrs]
  (util/visit-values attrs key->xform))
