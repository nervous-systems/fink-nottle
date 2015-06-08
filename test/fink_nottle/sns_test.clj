(ns fink-nottle.sns-test
  (:require [clojure.core.async :as async]
            [fink-nottle.sns :refer :all]
            [fink-nottle.test-util :refer :all]
            [clojure.test :refer :all]))

(defn create-platform-application*
  [& [{:keys [name] :or {name platform-app-name}}]]
  (create-platform-application!!
   creds
   :GCM
   name
   {:platform-credential gcm-api-key}))

(deftest ^:integration create-platform-application+
  (is (= "arn:" (-> (create-platform-application*)
                    (subs 0 4)))))

(deftest ^:integration delete-platform-application+
  (let [arn (create-platform-application* (random-name))]
    (is (delete-platform-application!! creds arn))))

(deftest ^:integration list-platform-applications+
  (let [arn (create-platform-application*)]
    (is ((->> (list-platform-applications!! creds)
              (map :arn)
              (into #{})) arn))))

(def gcm-token "XYZ")

(defn create-platform-endpoint* [arn]
  (create-platform-endpoint!! creds arn gcm-token))

(deftest ^:integration create-platform-endpoint+
  (let [p-arn (create-platform-application*)]
    (is (= "arn:" (-> (create-platform-endpoint* p-arn)
                      (subs 0 4))))))

(deftest ^:integration list-endpoints-by-platform-application+
  (let [p-arn (create-platform-application*)
        e-arn (create-platform-endpoint* p-arn)
        arns
        (->> (list-endpoints-by-platform-application!! creds p-arn)
             (map :arn)
             (into #{}))]
    (is (arns e-arn))))

(defn create-topic* []
  (create-topic!! creds :amazing-topic))

(deftest ^:integration create-topic+
  (is (= "arn:" (-> (create-topic*)
                    (subs 0 4)))))

(deftest ^:integration get-topic-attributes+
  (let [arn (create-topic*)]
    (is (map? (get-topic-attributes!! creds arn)))))

(deftest ^:integration get-endpoint-attributes+
  (let [p-arn (create-platform-application*)
        e-arn (create-platform-endpoint* p-arn)]
    (is (map? (get-endpoint-attributes!! creds e-arn)))))

(deftest ^:integration publish+
  (is (string?
       (publish!!
        creds
        {:default "OK"
         :email "This is the email"
         :gcm {:data {:message "This is the GCM"}
               :time-to-live 125
               :collapse-key "test"}
         :APNS_SANDBOX {:aps {:content-available 1}}}
        {:topic-arn (create-topic*)
         :subject "Hello"}))))

(deftest ^:integration subscribe+
  (let [p-arn (create-platform-application*)
        e-arn (create-platform-endpoint* p-arn)
        res (subscribe!! creds (create-topic*) :application e-arn)]
    (is (= "arn:" (subs res 0 4)))))
