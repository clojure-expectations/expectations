(defproject expectations "1.3.7"
  :description "testing framework"
  :jar-name "expectations.jar"
  :java-source-path "src/java"
  :source-path "src/clojure"
  :test-path "test/clojure"
  :dev-dependencies [[lein-expectations "0.0.1"]
                     [lein-publishers "1.0.4"]]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [junit/junit "4.8.1"]])
