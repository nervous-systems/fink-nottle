(ns fink-nottle.sns.consume
  (:require [camel-snake-kebab.core :as csk]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clojure.core.async :as async :refer [go <!]]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [eulalie.service-util :as service-util]
            [eulalie.util :as util]
            [glossop :refer [go-catching]])
  (:import [java.security Signature PublicKey]
           [java.security.cert CertificateFactory Certificate]))

(defn adjust-message-values [{:keys [type] :as m}]
  (assoc m :type (csk/->kebab-case-keyword type)))

(defn string->message [x]
  (-> (json/decode x csk/->kebab-case-keyword)
      adjust-message-values))

(defn stream->message [x & [{:keys [encoding] :or {encoding "UTF-8"}}]]
  (-> x
      (io/reader :encoding encoding)
      (json/parse-stream csk/->kebab-case-keyword)
      adjust-message-values))

(defn verify-cert-url [url expected-region]
  (let [url (cond-> url (string? url) url/url)
        reference (service-util/region->endpoint
                   expected-region {:service-name :sns})]
    (and (= (:protocol url) "https")
         (= (:host url) (:host reference)))))

(defn make-key-cache [] (atom {}))

(defn url->public-key! [url & [key-cache]]
  (go-catching
    (or (and key-cache (@key-cache url))
        (let [{:keys [body error]}
              (<! (util/channel-request! {:url (str url)}))]
          (if error
            error
            (let [pk (-> (CertificateFactory/getInstance "X.509")
                         (.generateCertificate body)
                         (.getPublicKey))]
              (when key-cache
                (swap! key-cache assoc url pk))
              pk))))))

(let [attrs [:message :message-id :subscribe-url :timestamp :token :topic-arn :type]
      renames {:subscribe-url "SubscribeURL"}]
  (defn bytes-to-sign [{:keys [type] :as m}]
    (let [m (cond-> m type (assoc :type (csk/->PascalCaseString type)))]
      (-> ^String
          (apply str
                 (for [k attrs :when (m k)]
                   (str (or (renames k) (csk/->PascalCaseString k))
                        "\n"
                        (m k)
                        "\n")))
          (.getBytes "UTF-8")))))

(defn verify? [pk {:keys [signature] :as m}]
  (let [sig (Signature/getInstance "SHA1withRSA")]
    (.initVerify sig ^PublicKey pk)
    (.update sig ^bytes (bytes-to-sign m))
    (.verify sig (b64/decode (.getBytes ^String signature "UTF-8")))))

(defn verify-message! [{:keys [signing-cert-url] :as m} region & [key-cache]]
  (go-catching
    (when (verify-cert-url signing-cert-url region)
      (let [pk (<! (url->public-key! signing-cert-url key-cache))]
        (verify? pk m)))))
