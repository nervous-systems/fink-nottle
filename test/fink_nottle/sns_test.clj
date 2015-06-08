(ns fink-nottle.sns-test
  (:require [fink-nottle.sns :refer :all]
            [clojure.test :refer :all]))

(def platform-app-name "the-best-app")
(def gcm-api-key (get (System/getenv) "GCM_API_KEY"))

(def creds
  {:access-key (get (System/getenv) "AWS_ACCESS_KEY")
   :secret-key (get (System/getenv) "AWS_SECRET_KEY")})

(defn create-platform-application* []
  (create-platform-application!!
   creds
   :GCM
   platform-app-name
   {:platform-credential gcm-api-key}))

(deftest ^:integration create-platform-application+
  (is (= "arn:" (-> (create-platform-application*)
                    (subs 0 4)))))

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

(defn create-topic* []
  (create-topic!! creds :amazing-topic))

(deftest ^:integration create-topic+
  (is (= "arn:" (-> (create-topic*)
                    (subs 0 4)))))

(deftest ^:integration get-topic-attributes+
  (let [arn (create-topic*)]
    ;; assert something?
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
        [tag res] (subscribe!! creds (create-topic*) :application e-arn)]
    (is (= :arn tag))
    (is (= "arn:" (subs res 0 4)))))
