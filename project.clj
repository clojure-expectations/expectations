(defproject expectations "2.1.5-SNAPSHOT"
  :description "testing framework"
  :jar-name "expectations.jar"
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :java-source-paths ["src/java"]
  :source-paths ["src/cljc" "src/clojure" "src/cljs"]
  :test-paths ["test/cljc" "test/clojure" "test"]

  :dependencies [[joda-time/joda-time "2.9.3"]
                 [junit/junit "4.12"]]

  :plugins [[lein-expectations "0.0.8"]
            [lein-publishers "1.0.13"]]

  :deploy-repositories [["releases" :clojars]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.8.34" :scope "provided"]]
                   :node-dependencies [[source-map-support "^0.2.9"]]
                   :plugins           [[lein-cljsbuild "1.0.5"]
                                       [lein-npm "0.5.0"]]}}

  :prep-tasks ["javac"]
  :auto-clean false

  :aliases {"test" ["expectations" "expectations.*" "success.*"]
            "test-fail" ["expectations" "failure.*"]}

  :cljsbuild {:builds [{:source-paths   ["src/cljs" "src/cljc" "test/cljs" "test/cljc"]
                        :notify-command ["node" "./target/out/test.js"]
                        :compiler       {:target         :nodejs
                                         :main           expectations.test
                                         :output-to      "target/out/test.js"
                                         :output-dir     "target/out"
                                         :optimizations  :none
                                         :cache-analysis true
                                         :source-map     true
                                         :pretty-print   true}}]}

  :min-lein-version "2.5.0")
