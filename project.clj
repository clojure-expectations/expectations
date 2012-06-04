(defproject expectations "1.4.4"
  :description "testing framework"
  :jar-name "expectations.jar"
  :java-source-path "src/java"
  :source-path "src/clojure"
  :test-path "test/clojure"
  :dev-dependencies [[lein-expectations "0.0.1"]
                     [lein-publishers "1.0.4"]
                     [joda-time/joda-time "2.1"]
                     [junit/junit "4.8.1"]]
  :dependencies [[org.clojure/clojure "1.3.0"]])
