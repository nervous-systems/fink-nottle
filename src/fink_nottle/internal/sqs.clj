(ns fink-nottle.internal.sqs
  (:require [clojure.set :as set]
            [clojure.walk :as walk]
            [fink-nottle.internal :as i]
            [fink-nottle.internal.util :as util]
            [fink-nottle.sqs.tagged :as tagged]
            [eulalie.sqs]
            [plumbing.core :as p]))

(def ->int  #(Integer/parseInt %))
(def ->long #(Long/parseLong %))

(def key->xform
  {:approximate-number-of-messages bigint
   :approximate-number-of-messages-not-visible bigint
   :approximate-number-of-messages-delayed bigint
   :approximate-receive-count bigint
   :approximate-first-receive-timestamp ->long

   :visibility-timeout ->int
   :created-timestamp ->long
   :last-modified-timestamp ->long
   :maximum-message-size ->int
   :maximum-retention-period ->int
   :delay-seconds ->int
   :receive-message-wait-time-seconds ->int
   :sent-timestamp ->long
   :sender-fault (partial = "true")})

(defn attr-val-out [x]
  (let [x (cond-> x (keyword? x) name)]
   (cond
     (string? x)          [:string x]
     (number? x)          [:number (str x)]
     (util/byte-array? x) [:binary (util/ba->b64-string x)]
     :else (throw (ex-info "bad-attr-type"
                           {:type :bad-attr-type :value x})))))

(defn attr-val-in [[tag value]]
  (case tag
    :string value
    :number (util/string->number value)
    :binary (util/b64-string->ba value)))

(defn augment-outgoing [{:keys [attrs body fink-nottle/tag] :as m}]
  (cond-> m tag
          (assoc
           :body (tagged/message-out tag body)
           :attrs (assoc attrs :fink-nottle-tag tag))))

(defmethod i/restructure-request [:sqs :send-message]
  [_ _ message]
  (let [{:keys [attrs] :as message} (augment-outgoing message)]
    (assoc message :attrs (p/map-vals attr-val-out attrs))))

(defmethod i/restructure-request [:sqs :send-message-batch]
  [_ _ {:keys [messages generate-ids] :as m}]
  (assoc m :messages
         (map-indexed
          (fn [i msg]
            (let [{:keys [attrs] :as msg} (augment-outgoing msg)]
              (cond-> (assoc msg :attrs (p/map-vals attr-val-out attrs))
                generate-ids (assoc :id (str i)))))
          messages)))

(defn augment-incoming [{{:keys [fink-nottle-tag]} :attrs body :body :as m}]
  (cond-> m fink-nottle-tag
          (assoc :body (tagged/message-in (keyword fink-nottle-tag) body))))

(defn restructure-message [{:keys [attrs] :as m}]
  (-> m
      (util/visit-values key->xform)
      (assoc :attrs (p/map-vals attr-val-in attrs))
      augment-incoming))

(defmethod i/restructure-response [:sqs :receive-message] [_ _ ms]
  (map restructure-message ms))

(defmethod i/restructure-response [:sqs :get-queue-attributes] [_ _ attrs]
  (util/visit-values attrs key->xform))
