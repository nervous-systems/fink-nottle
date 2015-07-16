(ns fink-nottle.sns.channeled-test
  ;; (:require [fink-nottle.sns.channeled :as channeled]
  ;;           [fink-nottle.sns :as sns]
  ;;           [fink-nottle.test-util :refer :all]
  ;;           [clojure.test :refer :all]
  ;;           [clojure.core.async :as async])
  )

;; Have another go at this

;; (defn subscribe-repeatedly* [n arn]
;;   (->> (range n)
;;        (map #(sns/subscribe! creds arn "email-json" (str "in@val.id." %)))
;;        async/merge
;;        (async/into [])
;;        async/<!!))

;; (deftest list-subscriptions-by-topic+
;;   (let [arn (sns/create-topic!! creds (random-name))]
;;     (try
;;       (subscribe-repeatedly* 101 arn)
;;       (let [results (->> arn
;;                          (channeled/list-subscriptions-by-topic! creds)
;;                          (async/into [])
;;                          async/<!!)]
;;         (is (= (count results) 101)))
;;       (finally
;;         (sns/delete-topic!! creds arn)))))
