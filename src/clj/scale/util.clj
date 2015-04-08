(ns scale.util
  (:require [scale.graph :as g]))

(defn effective-filter [date]
  (filter
    (fn [edge]
      (let [from (g/get-property edge "effective_start")]
        (and (or (nil? from) (<= from date))
             (if-let [to (g/get-property edge "effective_end")]
               (< date to)
               true))))))

(defn xform
  "Apply a transducer to get 1 value from 1 input"
  [f x]
  ((f (fn [_ y] y)) nil x))

