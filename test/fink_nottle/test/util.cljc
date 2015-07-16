(ns fink-nottle.test.util)

(defn env [s & [default]]
  #? (:clj
      (get (System/getenv) s default)
      :cljs
      (or (aget js/process "env" s) default)))

(def creds
  {:access-key (env "AWS_ACCESS_KEY")
   :secret-key (env "AWS_SECRET_KEY")})

(def platform-app-name "the-best-app")

(def gcm-api-key (env "GCM_API_KEY"))

(defn random-name []
  (str "fink-nottle-random-" (rand-int 0xFFFF)))
