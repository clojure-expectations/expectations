(ns expectations.junit-runner
  (:require expectations)
  (:import
    [java.io File]
    [org.junit.runner Runner Description]
    [org.junit.runner.notification Failure]
    [junit.framework AssertionFailedError ComparisonFailure]
    [java.lang.annotation Annotation]))

(def empty-ann-arr (make-array Annotation 0))

(defn start [notifier descs test-name]
  (.fireTestStarted notifier (descs (str test-name " ()"))))

(defn finish [notifier descs test-name]
  (.fireTestFinished notifier (descs (str test-name " ()"))))

(defn failure [notifier descs file-pos info]
  (println file-pos)
  (println info)
  (.fireTestFailure notifier
    (expectations.ExpectationsFailure. (descs (str file-pos " ()")) (str "failure in (" file-pos ")\n" info "\n"))))

(defn create-desc [accum v]
  (let [test-name (str (expectations/test-name (meta v)) " ()")]
    (assoc accum test-name (Description/createSuiteDescription test-name empty-ann-arr))))

(defn ignored-fns [{:keys [className fileName]}]
  (or (= fileName "expectations.clj")
      (re-seq #"clojure.lang" className)
      (re-seq #"clojure.core" className)
      (re-seq #"clojure.main" className)
      (re-seq #"expectations.junit_runner" className)
      (re-seq #"emark.ExpecatationsTestRunner" className)
      (re-seq #"com.intellij" className)
      (re-seq #"org.junit.runner.JUnitCore" className)
      (re-seq #"sun.reflect" className)
      (re-seq #"java.lang" className)))

(defn create-runner [source]
  (let [files (remove :hidden (->> source .testPath (File.) .listFiles (map bean)))
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
          (expectations/run-all-tests))]
          (.fireTestStarted notifier suite-description)
          (when (or (< 0 (:error results)) (< 0 (:fail results)))
            (.fireTestFailure notifier (expectations.ExpectationsFailure. suite-description "")))
          (.fireTestFinished notifier suite-description))))))