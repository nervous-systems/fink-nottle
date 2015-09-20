(ns fink-nottle.internal.util
  (:require [clojure.walk :as walk]
            [fink-nottle.internal.platform :as platform :refer [->int]]))

(defn attr-val-out [x]
  (let [x (cond-> x (keyword? x) name)]
   (cond
     (string? x)              [:string x]
     (number? x)              [:number (str x)]
     (platform/byte-array? x) [:binary (platform/ba->b64-string x)]
     :else (throw (ex-info "bad-attr-type"
                           {:type :bad-attr-type :value x})))))

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

