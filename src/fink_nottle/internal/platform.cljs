(ns fink-nottle.internal.platform
  (:require-macros [cljs.core :refer [instance?]]))

(def ->int js/parseInt)

(def BigNumber (nodejs/require "bignumber.js"))

(defn string->number [s]
  (let [v (BigNumber. s)]
    (if (and (= -1 (.indexOf s ".")) (<= (.precision v) 15))
      (js/parseInt s)
      v)))

(defn byte-array? [x]
  (instance? js/Buffer x))

(defn ba->b64-string [x]
  (.toString x "base64"))

(defn b64-string->ba [x]
  (js/Buffer. x "base64"))

