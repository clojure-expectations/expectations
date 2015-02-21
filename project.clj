(defproject expectations "2.0.15"
  :description "testing framework"
  :jar-name "expectations.jar"
  :java-source-paths ["src/java"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [junit/junit "4.8.1"]]
  :plugins [[lein-expectations "0.0.7"]
            [lein-publishers "1.0.11"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[joda-time/joda-time "2.1"]]}})
