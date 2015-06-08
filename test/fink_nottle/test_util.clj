(ns fink-nottle.test-util
  (:require [clojure.test :refer :all]))

(def creds
  {:access-key (get (System/getenv) "AWS_ACCESS_KEY")
   :secret-key (get (System/getenv) "AWS_SECRET_KEY")})

(def platform-app-name "the-best-app")
(def gcm-api-key (get (System/getenv) "GCM_API_KEY"))

(defn random-name []
  (str "fink-nottle-random-" (rand-int 0xFFFF)))
