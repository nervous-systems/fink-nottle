(ns fink-nottle.internal
  (:require [eulalie.core :as eulalie]
            [eulalie.support]))

(defmulti  restructure-response (fn [service target m] [service target]))
(defmethod restructure-response :default [_ _ m] m)

(defmulti  restructure-request (fn [service target m] [service target]))
(defmethod restructure-request :default [_ _ m] m)

(defmulti  parse-service-values (fn [service target m] service))
(defmethod parse-service-values :default [_ _ m] m)

(defn handle-response [service target resp]
  (->> resp
       (restructure-response service target)
       (parse-service-values service target)))

#? (:clj
    (defmacro defissuers [service-name t->args]
      `(do
         ~@(for [[target args] t->args]
             `(eulalie.support/defissuer ~service-name ~target ~args
                (partial restructure-request ~service-name)
                (partial handle-response ~service-name))))))
