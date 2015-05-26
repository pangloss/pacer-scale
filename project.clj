(defproject com.xnlogic/graph.scale "0.1.4"
  :description "Graph Scale or Timeline data structure"
  :url "http://xnlogic.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha6"]
                 [com.tinkerpop.blueprints/blueprints-core "2.6.0"]
                 [com.tinkerpop/pipes "2.6.0"]
                 [com.tinkerpop.gremlin/gremlin-java "2.6.0"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/test.check "0.7.0"]
                   [criterium "0.4.3"]]}
   :compiled
   {:source-paths      ["src/clj"]
    :java-source-paths ["src/java"]
    :aot :all
    :omit-source true}}
  :source-paths      ["src/clj" "dev"]
  :java-source-paths ["src/java"]
  ; lein test-refresh : rerun tests when files change
  :global-vars {*warn-on-reflection* true})

