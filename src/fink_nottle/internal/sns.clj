(ns fink-nottle.internal.sns
  (:require [cheshire.core :as json]
            [clojure.walk :as walk]
            [eulalie.util.xml :as xml]
            [fink-nottle.internal :as i]
            [fink-nottle.internal.util :as util]
            [glossop :refer [fn->]]))

(def ->bool (partial = "true"))

(def key->xform
  {:subscriptions-pending   bigint
   :subscriptions-confirmed bigint
   :subscriptions-deleted   bigint
   :success-feedback-sample-rate #(Integer/parseInt %)
   :enabled ->bool
   :confirmation-was-authenticated ->bool})

(doseq [target [:get-endpoint-attributes
                :get-subscription-attributes
                :get-topic-attributes]]
  (defmethod i/restructure-response [:sns target] [_ _ m]
    (util/visit-values m key->xform)))
