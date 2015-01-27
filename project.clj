(defproject expectations "2.1.0-SNAPSHOT"
  :description "testing framework"
  :jar-name "expectations.jar"
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :java-source-paths ["src/java"]
  :source-paths ["src/cljx" "src/clojure"]
  :test-paths ["target/test-classes"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [junit/junit "4.12"]]

  :plugins [[com.keminglabs/cljx "0.5.0"]
            [lein-cljsbuild "1.0.4"]
            ;[lein-expectations "0.0.8"]
            [lein-publishers "1.0.13"]]

  :deploy-repositories [["releases" :clojars]]

  :profiles {:dev {:dependencies [[joda-time/joda-time "2.7"]
                                  [org.clojure/clojurescript "0.0-2727"]]}}

  :prep-tasks ["clean" "cljx" "javac"]
  :auto-clean false

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}

                  {:source-paths ["test/cljx"]
                  :output-path "target/test-classes"
                  :rules :clj}

                  {:source-paths ["test/cljx"]
                  :output-path "target/test-classes"
                  :rules :cljs}]}

  :cljsbuild {:test-commands {"node" ["node" :node-runner "target/testable.js"]}
              :builds [{:source-paths ["target/classes" "target/test-classes"]
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :advanced
                                   :pretty-print true}}]}

  :min-lein-version "2.5.0")
