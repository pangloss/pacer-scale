(ns scale.graph
  (:import (com.tinkerpop.blueprints Graph Direction Edge Element Vertex)))

(defn out-edges
  ([label]
   (let [labels (into-array String [label])]
     (fn [^Vertex vertex]
       (.getEdges vertex Direction/OUT labels))))
  ([^Vertex vertex label]
   (.getEdges vertex Direction/OUT (into-array String [label]))))

(defn in-edges
  ([label]
   (let [labels (into-array String [label])]
     (fn [^Vertex vertex]
       (.getEdges vertex Direction/IN labels))))
  ([^Vertex vertex label]
   (.getEdges vertex Direction/IN (into-array String [label]))))

(defn out-vertex [^Edge edge]
  (.getVertex edge Direction/OUT))

(defn in-vertex [^Edge edge]
  (.getVertex edge Direction/IN))

(defn out-vertices [^Vertex v label]
  (.getVertices v Direction/OUT (into-array String [label])))

(defn in-vertices [^Vertex v label]
  ;(into-array String ["within_location"])
  (.getVertices v Direction/IN (into-array String [label])))

(defn get-property [^Element e name]
  (.getProperty e name))

(defn create-vertex [^Graph g]
  (.addVertex g nil))

(defn create-edge [^Graph g ^Vertex from ^Vertex to label]
  (.addEdge g nil from to label))

(defn set-property [^Element e name value]
  (.setProperty e name value))

(defn delete [^Element e]
  (.remove e))

(defn index [g type-name property]
  (.. g
      (getVertexType type-name)
      (getProperty property)
      index))

(defn lookup
  ([g index value]
   (when-let [id (.get index value)]
     (.getVertex g id)))
  ([g type-name property value]
   (lookup g (index g type-name property) value)))
