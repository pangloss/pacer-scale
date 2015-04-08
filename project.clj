(defproject scale "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [com.tinkerpop.blueprints/blueprints-core "2.6.0"]
                 [com.tinkerpop/pipes "2.6.0"]
                 [com.tinkerpop.gremlin/gremlin-java "2.6.0"]]
  :profiles
  {:dev {:dependencies [[org.clojure/test.check "0.7.0"]
                        [criterium "0.4.3"]]}}
  :source-paths      ["src/clj" "dev"]
  :java-source-paths ["src/java"]
  ; lein test-refresh : rerun tests when files change
  :global-vars {*warn-on-reflection* true}
  :plugins [[com.jakemccrary/lein-test-refresh "0.5.2"]])

