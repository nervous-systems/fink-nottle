(ns fink-nottle.internal.platform
  (:require [base64-clj.core :as base64])
  (:import (clojure.lang BigInt)
           (java.nio.charset Charset)))

(def ->int #(Long/parseLong %))

(defn string->number [^String s]
  (if (.contains s ".")
    (BigDecimal. s)
    (bigint (BigInteger. s))))

(defn- array-ctor->type-checker [t]
  (partial instance? (type (t []))))

(def byte-array? (array-ctor->type-checker byte-array))

(def utf-8 (Charset/forName "UTF-8"))

(defn ba->b64-string [^bytes x]
  (String. ^bytes (base64/encode-bytes x) ^Charset utf-8))

(defn b64-string->ba [^String x]
  (base64/decode-bytes (.getBytes x ^Charset utf-8)))
