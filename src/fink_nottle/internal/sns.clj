(ns fink-nottle.internal.sns
  (:require [camel-snake-kebab.core :as csk]
            [fink-nottle.internal :refer [restructure-response]]
            [eulalie.util.xml :as xml]
            [fink-nottle.internal.util :as util]
            [cheshire.core :as json]))

(defn application-arn [m]
  (xml/child-content m :platform-application-arn))

(defmethod restructure-response [:sns :create-platform-application] [_ _ m]
  (application-arn m))

(defn attrs->map [attributes]
  (-> (into {}
        (for [entry (xml/children attributes :entry)]
          [(csk/->kebab-case-keyword (xml/child-content entry :key))
           (xml/child-content entry :value)]))
      (update-in [:enabled] = "true")))

(defn flatten-application [{[arn {:keys [attributes]}] :member}]
  (-> attributes
      attrs->map
      (assoc :arn (application-arn arn))))

(defmethod restructure-response [:sns :list-platform-applications] [_ _ m]
  (with-meta
    (for [member (xml/children m :member)]
      (flatten-application member))
    {:next-token (xml/child-content m :next-token)}))

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

;; All this stuff ought to move into eulalie
(defmethod restructure-response [:sns :create-platform-endpoint] [_ _ m]
  (xml/child-content m :endpoint-arn))
(defmethod restructure-response [:sns :create-topic] [_ _ m]
  (xml/child-content m :topic-arn))
(defmethod restructure-response [:sns :publish] [_ _ m]
  (xml/child-content m :message-id))
(defmethod restructure-response [:sns :subscribe] [_ _ m]
  (let [result (xml/child-content m :subscribe-result)]
    (if (= result "pending confirmation")
      [:pending]
      [:arn (xml/child-content result :subscription-arn)])))
(defmethod restructure-response :confirm-subscription [_ _ m]
  (xml/child-content m :subscription-arn))
