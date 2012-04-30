(defproject expectations "1.3.9"
  :description "testing framework"
  :jar-name "expectations.jar"
  :java-source-path "src/java"
  :source-path "src/clojure"
  :test-path "test/clojure"
  :dev-dependencies [[lein-expectations "0.0.1"]
                     [lein-publishers "1.0.4"]]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [colorize "0.1.1"]
                 [junit/junit "4.8.1"]])
