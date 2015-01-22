(defproject expectations "2.0.17-SNAPSHOT"
  :description "testing framework"
  :jar-name "expectations.jar"
  :java-source-paths ["src/java"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [junit/junit "4.12"]]
  :plugins [[lein-expectations "0.0.8"]
            [lein-publishers "1.0.13"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[joda-time/joda-time "2.7"]]}})
