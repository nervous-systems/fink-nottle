(ns fink-nottle.internal.util
  (:require [clojure.core.async :as async]
            [clojure.data.codec.base64 :as b64]
            [clojure.walk :as walk])
  (:import (clojure.lang BigInt)
           (java.nio.charset Charset)))

(defn visit-values [x k->xform]
  (if (map? x)
    (into {}
      (for [[k v] x]
        (if-let [xform (k->xform k)]
          [k (xform v)]
          [k v])))
    x))

(defn walk-values [form k->xform]
  (walk/postwalk visit-values form))

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

(defn array-ctor->type-checker [t]
  (partial instance? (type (t []))))

(def byte-array? (array-ctor->type-checker byte-array))

(defn string->number [^String s]
  (if (.contains s ".")
    (BigDecimal. s)
    (bigint (BigInteger. s))))

(def utf-8 (Charset/forName "UTF-8"))

(defn ba->b64-string [^bytes x]
  (String. ^bytes (b64/encode x) ^Charset utf-8))

(defn b64-string->ba [^String x]
  (b64/decode (.getBytes x ^Charset utf-8)))
