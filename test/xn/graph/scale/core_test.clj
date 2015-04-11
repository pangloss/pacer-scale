(ns xn.graph.scale.core-test
  (:use xn.graph.scale.core
        criterium.core)
  (import java.math.BigDecimal
          (com.tinkerpop.blueprints Graph Direction Edge Element Vertex)
          com.tinkerpop.blueprints.impls.tg.TinkerGraph
          xn.graph.scale.ScaleRangePipe)
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

(defn big-traversal [from to multiplier]
  (let [from (* from (inc multiplier) (inc multiplier))
        to   (* to   (inc multiplier) (inc multiplier))
        steps (traversal-steps from to)]
    (= (- to from)
       (apply + steps))))

(defspec
  bigger-steps-add-up-to-traversal-distance
  100
  (prop/for-all [from gen/pos-int
                 to gen/pos-int
                 multiplier gen/pos-int]
                (big-traversal from to multiplier)))

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
    distances))

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



(let [g (TinkerGraph.)
      v0 (generate-scale g -500 500 0.1M)
      label (into-array String ["next_1"])
      actual (loop [actual {} v v0]
               (if v
                 (recur (assoc actual (value v) v) (first (.getVertices v Direction/OUT label)))
                 actual))]
  (defspec
    navagate-a-real-scale
    1000
    (prop/for-all
      [[start offset tolerance] (->> (gen/tuple gen/int gen/int gen/ratio)
                                     (gen/fmap
                                       (fn [[s o t]]
                                         (if (neg? t) [s o (- t)] [s o t])))
                                     (gen/such-that
                                       (fn [[s o t]]
                                         (and (<= -500 s 500)
                                              (<= -500 (+ s o (- t)) (+ s o t) 500)))))]
      (let [start (BigDecimal. (str start))
            s (actual start)
            expected (actual (+ start offset))
            tolerance (BigDecimal. (str (double tolerance)))
            f (scale-range -500 500 0.1M (BigDecimal. (str offset)) tolerance tolerance)
            r (f s)]
        (is (some #{expected} r)))))

  (deftest test-range-end
    (let [f (scale-range -500 500 0.1M 1000M 0M 0M)]
      (is (not (empty? (f v0))))
      (is (= [500M] (map value (f v0))))))

  (deftest test-range-pipe
    (let [pipe (ScaleRangePipe. -500 500 0.1M 550M 1M 5M)
          a (java.util.ArrayList.)]
      (.add a v0)
      (.setStarts pipe a)
      (is (= (into [] (map actual) (range 49M 55.01M 0.1M))
             (into [] (seq pipe)))))))


(defspec
  no-falling-off-the-head
  1000
  (prop/for-all
    [[low high] (->> (gen/tuple gen/pos-int gen/pos-int)
                     (gen/such-that (fn [[l h]] (not= l h)))
                     (gen/fmap (fn [[l h]] (if (< l h) [l h] [h l]))))]
    (let [g (TinkerGraph.)
          v0 (generate-scale g low high 1M)
          label (into-array String ["next_1"])
          ^Vertex vn (loop [vn v0 ^Vertex v v0]
                       (if v
                         (recur v
                                (first (.getVertices v Direction/OUT label)))
                         vn))]
      (every? (fn [n]
                (let [f (scale-range low high 1M (- n) 0M 0M)
                      data (doall (f vn))
                      expected (- high n)]
                  (is (= [expected] (map value data)))))
              (range 0M (BigDecimal. (str (- high low))))))))


(defspec
  no-falling-off-the-end
  100
  (prop/for-all
    [[low high] (->> (gen/tuple gen/int gen/int)
                     (gen/such-that (fn [[l h]] (not= l h)))
                     (gen/fmap (fn [[l h]] (if (< l h) [l h] [h l]))))]
    (let [g (TinkerGraph.)
          v0 (generate-scale g low high 1M)]
      (every? (fn [n]
                (let [f (scale-range low high 1M n 0M 0M)]
                  (is (= [(+ low n)] (map value (f v0))))))
              (range 0M (BigDecimal. (str (- high low))))))))




(deftest inline-tests
  (test #'tests))

(comment
  (drop 9900 (gen/sample
    (gen/fmap (fn [[a b c]] [(* a c) (* b c)])
              (gen/tuple gen/pos-int gen/pos-int gen/pos-int))
    10000))

  (gen/sample (gen/such-that #(not (zero? %)) gen-decimal) 100)
  (gen/sample (gen/such-that #(apply not= %) (gen/tuple gen/int gen/int)) 1000)

  (quick-bench (remove-oversized-steps {:min 0 :max 100 :step 1M} 0 [100 -1 -1 -1]))
  (bench (remove-oversized-steps {:min 0 :max 99 :step 1M} 0 [100 -1 -1 -1]))
  (quick-bench (traversal-steps 3608 943)))
