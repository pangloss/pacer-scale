(ns xn.graph.scale.core
  (:require [clojure.test :refer [is testing set-test]])
  (:import (com.tinkerpop.blueprints Graph Direction Edge Element Vertex)
           java.math.BigDecimal
           java.math.MathContext
           java.util.Iterator))

(defn- within [scale n]
  (when (and n (<= (:min scale) n (:max scale)))
    n))

(defn- scale-index [scale n]
  (if-let [n (within scale n)]
    (long (Math/round (/ (double (- n (:min scale)))
                         (double (:step scale)))))))

(defn- scale-point [scale idx]
  (when idx
    (let [n (+ (:min scale) (* (:step scale) idx))]
      (within scale n))))

(defn scale-vertex [^Graph g scale i]
  (let [v (.addVertex g nil)]
    (.setProperty ^Vertex v "value" (scale-point scale i))))

(defn scale-edge [^Graph g ^Vertex from ^Vertex to dist]
  (.addEdge g nil from to (str "next_" dist)))

(defrecord scale [min max step offset below above])

(defn generate-scale [g min max step]
  (let [scale {:min min :max max :step step}
        dists [1 10 100 1000 10000 100000 1000000]
        v (scale-vertex g scale 0)
        last-idx (scale-index scale max)]
    (when last-idx
      (loop [idx 1 vs (transient (zipmap dists (repeat v)))]
        (let [v (scale-vertex g scale idx)
              vs (reduce (fn [vs dist]
                           (if (zero? (mod idx dist))
                             (do (scale-edge g (vs dist) v dist)
                                 (assoc! vs dist v))
                             vs))
                         vs
                         dists)]
          (when (< idx last-idx)
            (recur (inc idx) vs)))))))

(defn value [^Vertex point]
  (.getProperty point "value"))

(defn round-down [scale n]
  (- n (mod n (:step scale))))

(defn- round-up [scale n]
  (+ n (- (:step scale) (mod (+ n) (:step scale)))))

(defn- max-value [scale point]
  (let [n (+ (value point)
             (:offset scale)
             (:above scale))]
    (within scale (round-down scale n))))

(defn- min-value [scale point]
  (let [n (+ (value point)
             (:offset scale)
             (- (:below scale)))]
    (within scale (round-up scale n))))

(defmacro inspect [n]
  `(let [n# ~n]
     (prn '~n :=> n#)
     n#))

(defn ^long next-step [^long current ^long target ^long step]
  (let [step-mult (long 10)
        max-step (long 100000)
        step (long step)]
    (if (< (/ (double (Math/abs (- target current))) (double step)) 0.6666)
      (if (< current target)
        1
        -1)
      (let [relative (mod current step)]
        (cond
          (zero? relative)
          (cond
            (not= step max-step)
            (let [^long bigger (next-step current target (* step step-mult))]
              (cond (> (Math/abs bigger) (Math/abs step)) bigger
                    (pos? bigger) step
                    :else (- step)))
            (< current target) step
            :else (- step))
          (< relative (/ step 2.0))
          -1
          :else
          1)))))

(defn traversal-steps [current target]
  (loop [steps (transient []) current (long current)]
    (if (or (= current target) (< 1000 (count steps)))
      (persistent! steps)
      (let [^long step (next-step (long current) (long target) (long 10))]
        (+ current step)
        (recur (conj! steps step) (+ current step))))))


(defmulti ^:private traversal-step (fn [point n] n))

(defn- *traversal-step [^Vertex point edge-dir ^String edge-label vertex-dir]
  (let [^Iterator edges (.getEdges point edge-dir edge-label)
        ^Edge edge (when (.hasNext edges) (.next edges))]
    (.getVertex edge vertex-dir)))

(defmethod traversal-step 1 [^Vertex point n]
  (*traversal-step point Direction/OUT "next_1" Direction/IN))
(defmethod traversal-step -1 [^Vertex point n]
  (*traversal-step point Direction/IN "next_1" Direction/OUT))
(defmethod traversal-step 10 [^Vertex point n]
  (*traversal-step point Direction/OUT "next_10" Direction/IN))
(defmethod traversal-step -10 [^Vertex point n]
  (*traversal-step point Direction/IN "next_10" Direction/OUT))
(defmethod traversal-step 100 [^Vertex point n]
  (*traversal-step point Direction/OUT "next_100" Direction/IN))
(defmethod traversal-step -100 [^Vertex point n]
  (*traversal-step point Direction/IN "next_100" Direction/OUT))
(defmethod traversal-step 1000 [^Vertex point n]
  (*traversal-step point Direction/OUT "next_1000" Direction/IN))
(defmethod traversal-step -1000 [^Vertex point n]
  (*traversal-step point Direction/IN "next_1000" Direction/OUT))
(defmethod traversal-step 10000 [^Vertex point n]
  (*traversal-step point Direction/OUT "next_10000" Direction/IN))
(defmethod traversal-step -10000 [^Vertex point n]
  (*traversal-step point Direction/IN "next_10000" Direction/OUT))

; FIXME: first If the first point is off the scale but the desired range includes the scale, it should adjust.
(defn- first-point
  "Get the first point in the desired range. May be null if the desired point is off the scale."
  [scale point]
  (let [current (scale-index scale (value point))
        target (scale-index scale (min-value scale point))]
    (when (and current target)
      (reduce traversal-step
              point
              (traversal-steps current target)))))

(defn- next-point [scale max-value]
  (fn [point]
    (*traversal-step point Direction/OUT "next_1" Direction/IN)))

(defn scale-range
  ([scale offset below above]
   {:pre [(decimal? offset)
          (decimal? (:step scale))
          (decimal? below)
          (decimal? above)]}
   (let [scale (assoc scale :offset offset :below below :above above)]
     (fn [point]
       (->> (first-point scale point)
            (iterate (next-point scale (max-value scale point)))
            (take-while some?)))))
  ([min max step offset below above]
   (scale-range {:min min :max max :step step}
                offset below above)))


; -------------------- TESTS ----------------------------------
; -------------------- TESTS ----------------------------------
; -------------------- TESTS ----------------------------------

(set-test
  scale-index
  (let [s {:min 1 :max 8 :step 0.2M}]
    (is (= 0 (scale-index s 1M)))
    (is (= 1 (scale-index s 1.2M)))
    (is (= 35 (scale-index s 8)))
    (is (nil? (scale-index s -800)))
    (is (nil? (scale-index s nil)))
    (is (nil? (scale-index s 800)))))

(set-test
  scale-point
  (let [s {:min 1 :max 8 :step 0.2M}]
    (is (= 1M (scale-point s 0)))
    (is (= 1.2M (scale-point s 1)))
    (is (= 8M (scale-point s 35)))
    (is (nil? (scale-point s -800)))
    (is (nil? (scale-point s nil)))
    (is (nil? (scale-point s 800)))))

(set-test
  min-value
  (with-redefs [value identity]
    (let [s {:min 0 :max 8 :step 0.2M
             :offset 3.5M :below 1M :above 1M}]
      (is (= 3.6M (min-value s 1)))
      (is (= 7.6M (min-value s 5)))
      (is (nil? (min-value (assoc s :below 6M) 2)))
      (is (= 0.6M (min-value (assoc s :below 6M) 3))))))

(set-test
  max-value
  (with-redefs [value identity]
    (let [s {:min 0 :max 8 :step 0.2M
             :offset 3.5M :below 1M :above 1M}]
      (is (= 5.4M (max-value s 1)))
      (is (nil? (max-value s 5))))))

(set-test next-step
  (do
    (is (= -100 (next-step 100 -17 10)))

    (is (= 1 (next-step 349 351 10)))
    (is (= 1 (next-step 350 351 10)))
    (is (= -1 (next-step 352 351 10)))
    (is (= -1 (next-step 353 351 10)))

    (is (= 1 (next-step 199 351 10)))
    (is (= -1 (next-step 201 351 10)))
    (is (= -10 (next-step 220 351 10)))
    (is (= -10 (next-step 240 351 10)))
    (is (= 10 (next-step 250 351 10)))
    (is (= 1 (next-step 299 351 10)))
    (is (= 10 (next-step 300 351 10)))
    (is (= 1 (next-step 350 351 10)))
    (is (= -10 (next-step 400 351 10)))
    (is (= -1 (next-step 441 351 10)))
    (is (= 1 (next-step 455 351 10)))

    (is (= 100 (next-step 100 351 10)))
    (is (= -10 (next-step 440 351 10)))
    (is (= 10 (next-step 450 351 10)))
    (is (= -100 (next-step 500 351 10)))
    (is (= -1 (next-step 501 351 10)))))

(set-test
  traversal-steps
  (testing "variations"
    (is (= [] (traversal-steps 0 0)))
    (is (= [1] (traversal-steps 0 1)))
    (is (= [10] (traversal-steps 0 10)))
    (is (= [-1 10] (traversal-steps 1 10)))
    (is (= [-1 10 1] (traversal-steps 1 11)))
    (is (= [1 1] (traversal-steps 9 11)))
    (is (= [1 1 1 1 1 10 10 10 10 100 100 100 10 10 10 10 10 1 1 1 1 1]
           (traversal-steps 55 455)))
    (is (= [1 1 1 1 1 10 10 10 10 100 100 100 100 -10 -10 -1 -1 -1 -1 -1]
           (traversal-steps 55 475)))
    (is (= [1 1 1 1 1 10 10 10 10 10 -100 -10 -10 1 1 1]
           (traversal-steps 45 -17)))))

(def tests)
(set-test tests
  (testing "tests defined in scale.core itself"
    (test #'min-value)
    (test #'max-value)
    (test #'scale-index)
    (test #'next-step)
    (test #'scale-point)))
