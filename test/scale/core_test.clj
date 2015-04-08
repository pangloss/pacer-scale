(ns scale.core-test
  (:use scale.core
        criterium.core)
  (import java.math.BigDecimal)
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defspec
  steps-add-up-to-traversal-distance
  100 ;; the number of iterations for test.check to test
  (prop/for-all [from gen/pos-int
                 to gen/pos-int]
                (= (- to from)
                   (apply + (traversal-steps from to)))))


(defspec
  bigger-steps-add-up-to-traversal-distance
  100
  (prop/for-all [from gen/pos-int
                 to gen/pos-int
                 multiplier gen/pos-int]
                (let [from (* from (inc multiplier))
                      to (* to (inc multiplier))]
                  (= (- to from)
                     (apply + (traversal-steps from to))))))

(def gen-decimal
  (->> gen/ratio
       (gen/fmap #(try
                    (BigDecimal. (str (float (if (neg? %) (* -1 %) %))))
                    (catch ArithmeticException e nil)))
       (gen/such-that some?)))

(defn gen-nonzero [gen]
  (gen/such-that #(not (zero? %)) gen))

(defn predicted-edges [max-idx]
  (reduce
    (fn [m d]
      (if-let [data (->> (range 0 max-idx)
                         (filter #(= 0 (mod % d)))
                         (partition 2 1)
                         seq)]
        (assoc m d data)
        m))
    {}
    [1 10 100 1000 10000 100000 1000000]))

(defspec
  generate-various-scales
  100
  (prop/for-all
    [[from to step]
     (->> (gen/tuple gen/int gen/int (gen-nonzero gen-decimal) gen/int)
          (gen/fmap (fn [[from to step mult]]
                      [(* from mult) (* to mult) step]))
          (gen/such-that (fn [[from to]] (not= from to)))
          (gen/fmap
            (fn [[from to step]]
              (if (< from to)
                [from to step]
                [to from step])))
          (gen/fmap
            (fn [[from to step]]
              (loop [from from to to step step]
                (if (<= (- to from) step)
                  (recur from to (/ step 2))
                  [from to step])))))]
    (let [vertices (atom (transient []))
          edges (atom (transient {}))]
      (with-redefs [scale-vertex
                    (fn [g scale i]
                      (swap! vertices conj! i)
                      i)
                    scale-edge
                    (fn [g from to dist]
                      (swap! edges assoc! dist (conj (@edges dist []) [from to])))]
        (generate-scale nil from to step)
        (let [max-idx (inc (long (Math/round (/ (double (- to from)) (double step)))))]
          ;(prn [from to step] max-idx)
          (is (= (range 0 max-idx)
                 (persistent! @vertices)))
          (is (= (predicted-edges max-idx)
                 (persistent! @edges))))))))




(deftest inline-tests
  (test #'tests))

(comment
  (drop 9900 (gen/sample
    (gen/fmap (fn [[a b c]] [(* a c) (* b c)])
              (gen/tuple gen/pos-int gen/pos-int gen/pos-int))
    10000))

  (gen/sample (gen/such-that #(not (zero? %)) gen-decimal) 100)
  (gen/sample (gen/such-that #(apply not= %) (gen/tuple gen/int gen/int)) 1000)

  (quick-bench (traversal-steps 3608 943)))
