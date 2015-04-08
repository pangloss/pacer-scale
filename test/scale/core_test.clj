(ns scale.core-test
  (:use scale.core
        criterium.core)
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defspec
  steps-add-up-to-traversal-distance
  1000 ;; the number of iterations for test.check to test
  (prop/for-all [from gen/pos-int
                 to gen/pos-int]
                (= (- to from)
                   (apply + (traversal-steps from to)))))


(defspec
  bigger-steps-add-up-to-traversal-distance
  100000
  (prop/for-all [from gen/pos-int
                 to gen/pos-int
                 multiplier gen/pos-int]
                (let [from (* from (inc multiplier))
                      to (* to (inc multiplier))]
                  (= (- to from)
                     (apply + (traversal-steps from to))))))


(comment
  (drop 9900 (gen/sample
    (gen/fmap (fn [[a b c]] [(* a c) (* b c)])
              (gen/tuple gen/pos-int gen/pos-int gen/pos-int))
    10000))

  (quick-bench (traversal-steps 3608 943)))

(deftest inline-tests
  (test #'tests))
