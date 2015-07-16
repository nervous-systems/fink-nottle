(ns fink-nottle.internal.sns
  (:require [eulalie.sns]
            [fink-nottle.internal :as i]
            [fink-nottle.internal.platform :refer [->int]]
            [fink-nottle.internal.util :as util]))

(def key->xform
  {:subscriptions-pending   ->int
   :subscriptions-confirmed ->int
   :subscriptions-deleted   ->int
   :success-feedback-sample-rate ->int
   :enabled util/->bool
   :confirmation-was-authenticated util/->bool})

(doseq [target [:get-endpoint-attributes
                :get-subscription-attributes
                :get-topic-attributes]]
  (defmethod i/restructure-response [:sns target] [_ _ m]
    (util/visit-values m key->xform)))
