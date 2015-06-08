(ns fink-nottle.internal.util)

(defn parse-numeric-keys [m ks]
  (reduce
   (fn [m k]
     (if-let [v (m k)]
       (assoc m k (bigint v))
       m))
   m ks))
