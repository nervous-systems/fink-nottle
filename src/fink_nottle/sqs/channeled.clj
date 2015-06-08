(ns fink-nottle.sqs.channeled
  (:require [fink-nottle.sqs :as sqs]
            [fink-nottle.internal.util :as util]
            [clojure.core.async :as async]
            [glossop :as g :refer [<?]]))

(defn receive-message! [queue-url & [{:keys [chan] :as params}]]
  (let [params (merge {:maximum 10 :wait-seconds 20} params)
        chan   (or chan (async/chan 10))]
    (g/go-catching
      (loop []
        (let [messages (-> (sqs/receive-message! queue-url params) <? not-empty)]
          (if (or (not messages) (<? (util/onto-chan? chan messages)))
            (recur)
            (async/close! chan)))))))
