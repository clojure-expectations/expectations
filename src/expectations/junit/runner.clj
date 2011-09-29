(ns expectations.junit.runner
  (:require expectations)
  (:import
    [java.io File]
    [org.junit.runner Runner Description]
    [org.junit.runner.notification Failure]
    [junit.framework AssertionFailedError ComparisonFailure]
    [java.lang.annotation Annotation]))

(def empty-ann-arr (make-array Annotation 0))

(defn format-test-name [test-name test-meta]
  (str test-name " (" (:file test-meta) ":" (:line test-meta) " " (:name test-meta) ")"))

(defn start [notifier descs test-name test-meta]
  (.fireTestStarted notifier (descs (format-test-name test-name test-meta))))

(defn finish [notifier descs test-name test-meta]
  (.fireTestFinished notifier (descs (format-test-name test-name test-meta))))

(defn failure [notifier descs test-name test-meta info]
  (.fireTestFailure notifier
    (expectations.junit.ExpectationsFailure. (descs (format-test-name test-name test-meta)) (str "failure in (" (expectations/test-file test-meta) ") : " (:ns test-meta) "\n" info "\n"))))

(defn create-desc [accum v]
  (let [test-name (format-test-name (expectations/test-name (meta v)) (meta v))]
    (assoc accum test-name (Description/createSuiteDescription test-name empty-ann-arr))))

(defn ignored-fns [{:keys [className fileName]}]
  (or (= fileName "expectations.clj")
    (re-seq #"clojure.lang" className)
    (re-seq #"clojure.core" className)
    (re-seq #"clojure.main" className)
    (re-seq #"expectations.junit.runner" className)
    (re-seq #"expectations.junit.ExpectationsTestRunner" className)
    (re-seq #"com.intellij" className)
    (re-seq #"org.junit.runner.JUnitCore" className)
    (re-seq #"org.junit.runners" className)
    (re-seq #"sun.reflect" className)
    (re-seq #"java.lang" className)))

(defn clj-file? [file-name]
  (re-seq #".clj$" (:absolutePath file-name)))

(defn create-runner [source]
  (let [files (->> source .testPath File. file-seq (map bean) (remove :hidden) (remove :directory) (filter clj-file?))
        _ (doseq [{:keys [absolutePath]} files] (load-file absolutePath))
        file-names (set (map :absolutePath files))
        suite-description (Description/createSuiteDescription (-> source class .getName) empty-ann-arr)
        vars-by-ns (map #(vals (ns-interns %)) (all-ns))
        all-vars (reduce into [] vars-by-ns)
        expectation-in-loaded-file? (fn [v] (and (file-names (:file (meta v))) (:expectation (meta v))))
        filtered-vars (filter expectation-in-loaded-file? all-vars)
        descs (reduce create-desc {} filtered-vars)]
    (doseq [desc (vals descs)] (.addChild suite-description desc))
    (proxy [Runner] []
      (getDescription [] suite-description)
      (run [notifier]
        (expectations/disable-run-on-shutdown)
        (let [results (binding [expectations/started (partial start notifier descs)
                                expectations/finished (partial finish notifier descs)
                                expectations/fail (partial failure notifier descs)
                                expectations/ignored-fns ignored-fns
                                expectations/summary (fn [_])]
          (expectations/run-tests-in-vars filtered-vars))]
          (.fireTestStarted notifier suite-description)
          (when (or (< 0 (:error results)) (< 0 (:fail results)))
            (.fireTestFailure notifier (expectations.junit.ExpectationsFailure. suite-description "")))
          (.fireTestFinished notifier suite-description))))))