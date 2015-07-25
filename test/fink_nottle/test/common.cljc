(ns fink-nottle.test.common
  (:require [eulalie.util :refer [env!]]))

(def creds
  {:access-key (env! "AWS_ACCESS_KEY")
   :secret-key (env! "AWS_SECRET_KEY")})

(def platform-app-name "the-best-app")

(def gcm-api-key (env! "GCM_API_KEY"))

(defn random-name []
  (str "fink-nottle-random-" (rand-int 0xFFFF)))
