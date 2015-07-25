(ns fink-nottle.test.sqs.util
  (:require [eulalie.core :as eulalie]
            [fink-nottle.test.common :refer [creds]]
            [glossop.core #? (:clj :refer :cljs :refer-macros) [go-catching <?]]))

;; whole bunch of junk copied from eulalie/test - fix

(defn issue-raw! [req]
  (go-catching
    (let [{:keys [error] :as resp} (<? (eulalie/issue-request! req))]
      (if (not-empty error)
        ;; ex-info doesn't print to anything useful in cljs
        (throw #? (:clj
                   (ex-info  (pr-str error) error)
                   :cljs
                   (js/Error (pr-str error))))
        resp))))

(defn sqs! [target content & [req-overrides]]
  (go-catching
    (let [req (merge
               {:service :sqs
                :target  target
                :max-retries 0
                :body content
                :creds creds}
               req-overrides)]
      (:body (<? (issue-raw! req))))))

(defn get-queue-url! [q-name]
  (sqs! :get-queue-url {:queue-name q-name}))

(defn create-queue! [q-name]
  (go-catching
    (try
      (<? (sqs! :create-queue {:queue-name q-name}))
      (catch
          #? (:clj clojure.lang.ExceptionInfo :cljs js/Error) e
          (if (-> e ex-data :type (= :queue-already-exists))
            (<? (get-queue-url! q-name))
            (throw e))))))

(defn delete-queue! [queue] (sqs! :delete-queue {:queue-url queue}))

(defn with-transient-queue! [f]
  (go-catching
    (let [q-name (str "fink-nottle-transient-" (rand-int 0xFFFF))
          q-url  (<? (create-queue! q-name))]
      (try
        (<? (f {:name q-name :url q-url}))
        (finally
          (<? (delete-queue! q-url)))))))
