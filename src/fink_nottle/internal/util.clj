(ns fink-nottle.internal.util
  (:require [clojure.core.async :as async]))

(defn parse-numeric-keys [m ks]
  (reduce
   (fn [m k]
     (if-let [v (m k)]
       (assoc m k (bigint v))
       m))
   m ks))

(defn onto-chan?
  "This is a version of onto-chan which never closes the target
  channel, and returns a boolean on its own channel, indicating
  whether all puts were completed"
  ([ch coll]
   (async/go-loop [vs (seq coll)]
     (if vs
       (when (async/>! ch (first vs))
         (recur (next vs)))
       true))))
