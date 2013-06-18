(defproject expectations "1.4.48"
  :description "testing framework"
  :jar-name "expectations.jar"
  :java-source-paths ["src/java"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [erajure/erajure "0.0.3"]
                 [junit/junit "4.8.1"]]
  :plugins [[lein-expectations "0.0.7"]
            [lein-publishers "1.0.4"]]
  :profiles {:dev {:dependencies [[joda-time/joda-time "2.1"]]}})
