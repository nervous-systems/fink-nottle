(ns fink-nottle.internal.sns
  (:require [camel-snake-kebab.core :as csk]
            [fink-nottle.internal :refer [restructure-response]]
            [eulalie.util.xml :as xml]
            [fink-nottle.internal.util :as util]
            [cheshire.core :as json]
            [glossop :refer [fn->]]))

;; Most of this stuff ought to move into eulalie, like SQS

(defn application-arn [m] (xml/child-content m :platform-application-arn))
(defn endpoint-arn    [m] (xml/child-content m :endpoint-arn))

(defmethod restructure-response [:sns :create-platform-application] [_ _ m]
  (application-arn m))

(defn attrs->map
  [attributes & [{:keys [parent] :or {parent :entry}}]]
  (let [{:keys [enabled] :as m}
        (into {}
          (for [entry (xml/children attributes parent)]
            [(csk/->kebab-case-keyword (xml/child-content entry :key))
             (xml/child-content entry :value)]))]
    (cond-> m enabled (assoc :enabled (= enabled "true")))))

(defn flatten-application [{[arn {:keys [attributes]}] :member}]
  (-> attributes
      attrs->map
      (assoc :arn (application-arn arn))))

(defn restructure-member-list [m apply-me]
  (with-meta
    (for [member (xml/children m :member)]
      (apply-me member))
    {:next-token (xml/child-content m :next-token)}))

(defmethod restructure-response [:sns :list-platform-applications] [_ _ m]
  (restructure-member-list m flatten-application))

(defmethod restructure-response [:sns :list-endpoints-by-platform-application] [_ _ m]
  (restructure-member-list
   m
   (fn [endpoint]
     (assoc (attrs->map endpoint) :arn
            (endpoint-arn endpoint)))))

(defmethod restructure-response [:sns :get-endpoint-attributes] [_ _ m]
  (attrs->map (xml/child m :attributes)))

(defmethod restructure-response [:sns :get-topic-attributes] [_ _ m]
  (-> (xml/child m :attributes)
      attrs->map
      (update-in [:effective-delivery-policy] json/decode true)
      (update-in [:policy] json/decode true)
      (util/parse-numeric-keys #{:subscriptions-pending
                                 :subscriptions-confirmed
                                 :subscriptions-deleted})))

(defmethod restructure-response [:sns :create-platform-endpoint] [_ _ m]
  (xml/child-content m :endpoint-arn))
(defmethod restructure-response [:sns :create-topic] [_ _ m]
  (xml/child-content m :topic-arn))
(defmethod restructure-response [:sns :publish] [_ _ m]
  (xml/child-content m :message-id))
(defmethod restructure-response [:sns :subscribe] [_ _ m]
  (let [result (xml/child-content m :subscription-arn)]
    (if (= result "pending confirmation")
      :fink-nottle/pending
      result)))

(defn fix-subscription-arn [x]
  (if (= x "PendingConfirmation") :fink-nottle/pending x))

(defmethod restructure-response [:sns :confirm-subscription] [_ _ m]
  (fix-subscription-arn (xml/child-content m :subscription-arn)))

(def subscription-attrs #{:protocol :owner :topic-arn :subscription-arn :endpoint})

(defn restructure-subscription [m]
  (-> m
      (xml/child-content->map subscription-attrs)
      (update-in [:subscription-arn] fix-subscription-arn)))

(defn restructure-subscriptions [resp]
  (map restructure-subscription (xml/children resp :member)))

(defmethod restructure-response [:sns :list-subscriptions] [_ _ m]
  (restructure-member-list m restructure-subscription))
(defmethod restructure-response [:sns :list-subscriptions-by-topic] [_ _ m]
  (restructure-member-list m restructure-subscription))
