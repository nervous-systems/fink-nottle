(ns fink-nottle.internal
  (:require [camel-snake-kebab.core :as csk]
            [plumbing.map]
            [clojure.core.async :as async]
            [eulalie.sns :as sns]
            [cheshire.core :as json]
            [glossop :refer [go-catching <? <?!]]))

(defn application-arn [m]
  (sns/child-content m :platform-application-arn))

(defmulti  flatten-response (fn [resp-type m] resp-type))
(defmethod flatten-response :default [_ m] m)
(defmethod flatten-response :create-platform-application [_ m]
  (application-arn m))

(defn attrs->map [attributes]
  (-> (into {}
        (for [entry attributes]
          [(csk/->kebab-case-keyword (sns/child-content entry :key))
           (sns/child-content entry :value)]))
      (update-in [:enabled] = "true")))

(defn flatten-application [{[arn {:keys [attributes]}] :member}]
  (-> attributes
      attrs->map
      (assoc :arn (application-arn arn))))

(defmethod flatten-response :list-platform-applications [_ m]
  (let [res (get-in m [:list-platform-applications-response
                       0
                       :list-platform-applications-result
                       0])]
    (with-meta
      (for [member (:platform-applications res)]
        (flatten-application member))
      {:next-token (:next-token res)})))

(defmethod flatten-response :get-endpoint-attributes [_ m]
  (attrs->map (sns/child m :attributes)))

(defn parse-numeric-keys [m ks]
  (reduce
   (fn [m k]
     (if-let [v (m k)]
       (assoc m k (bigint v))
       m))
   m ks))

(defmethod flatten-response :get-topic-attributes [_ m]
  (-> (sns/child m :attributes)
      attrs->map
      (update-in [:effective-delivery-policy] json/decode true)
      (update-in [:policy] json/decode true)
      (parse-numeric-keys #{:subscriptions-pending
                            :subscriptions-confirmed
                            :subscriptions-deleted})))

(defmethod flatten-response :create-platform-endpoint [_ m]
  (sns/child-content m :endpoint-arn))
(defmethod flatten-response :create-topic [_ m]
  (sns/child-content m :topic-arn))
(defmethod flatten-response :publish [_ m]
  (sns/child-content m :message-id))
(defmethod flatten-response :subscribe [_ m]
  (let [result (sns/child-content m :subscribe-result)]
    (if (= result "pending confirmation")
      [:pending]
      [:arn (sns/child-content result :subscription-arn)])))
(defmethod flatten-response :confirm-subscription [_ m]
  (sns/child-content m :subscription-arn))

(defn issue-targeted-request! [target creds req-body]
  (go-catching
    (let [{:keys [body error]}
          (<? (eulalie/issue-request!
               {:service :sns
                :creds   creds
                :target  target
                :body    req-body}))]
      (if-let [{:keys [type]} error]
        (ex-info (name type) error)
        (flatten-response target body)))))

(defmacro defissuer [target-name args & [doc]]
  (let [fname!  (-> target-name (str "!")  symbol)
        fname!! (-> target-name (str "!!") symbol)
        args'   (into '[creds] (conj args '& '[extra]))
        body    `(issue-targeted-request!
                  ~(keyword target-name) ~'creds
                  (merge (plumbing.map/keyword-map ~@args) ~'extra))]
    `(do
       (defn ~fname!  {:doc ~doc} ~args' ~body)
       (defn ~fname!! {:doc ~doc} ~args' (<?! ~body)))))

(defmacro defissuers [t->args]
  `(do
     ~@(for [[target args] t->args]
         `(defissuer ~target ~args))))
