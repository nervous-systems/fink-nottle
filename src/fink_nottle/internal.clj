(ns fink-nottle.internal
  (:require [plumbing.map]
            [clojure.core.async :as async]
            [cheshire.core :as json]
            [glossop :refer [go-catching <? <?!]]))

(defmulti  restructure-response (fn [service target m] [service target]))
(defmethod restructure-response :default [_ _ m] m)

(defmulti  restructure-request (fn [service target m] [service target]))
(defmethod restructure-request :default [_ _ m] m)

(defn issue-targeted-request! [service target creds req-body]
  (go-catching
    (let [{:keys [body error]}
          (<? (eulalie/issue-request!
               {:service service
                :creds   creds
                :target  target
                :body    (restructure-request service target req-body)}))]
      (if-let [{:keys [type]} error]
        (ex-info (name type) error)
        (restructure-response service target body)))))

(defmacro defissuer [target-name service-name args & [doc]]
  (let [fname!  (-> target-name (str "!")  symbol)
        fname!! (-> target-name (str "!!") symbol)
        args'   (into '[creds] (conj args '& '[extra]))
        body    `(issue-targeted-request!
                  ~service-name
                  ~(keyword target-name) ~'creds
                  (merge (plumbing.map/keyword-map ~@args) ~'extra))]
    `(do
       (defn ~fname!  {:doc ~doc} ~args' ~body)
       (defn ~fname!! {:doc ~doc} ~args' (<?! ~body)))))

(defmacro defissuers [service-name t->args]
  `(do
     ~@(for [[target args] t->args]
         `(defissuer ~target ~service-name ~args))))
