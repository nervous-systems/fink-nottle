(ns fink-nottle.internal.util
  (:require [clojure.walk :as walk]
            [fink-nottle.internal.platform :refer [->int]]))

(defn visit-values [x k->xform]
  (if (map? x)
    (into {}
      (for [[k v] x]
        (if-let [xform (k->xform k)]
          [k (xform v)]
          [k v])))
    x))

(defn walk-values [form k->xform]
  (walk/postwalk #(visit-values % k->xform) form))

(defn parse-numeric-keys [m ks]
  (reduce
   (fn [m k]
     (if-let [v (m k)]
       (assoc m k (->int v))
       m))
   m ks))

(def ->bool (partial = "true"))

