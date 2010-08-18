(ns expectations
  (:require [clojure.stacktrace :as stack]))

;;; GLOBALS USED BY THE REPORTING FUNCTIONS

(def *report-counters* nil)	  ; bound to a ref of a map in test-ns

(def *initial-report-counters*  ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0})

;;; UTILITIES FOR REPORTING FUNCTIONS

(defn file-position []
  (let [^StackTraceElement s (nth (.getStackTrace (new java.lang.Throwable)) 4)]
    (str (.getFileName s) ":" (.getLineNumber s))))

(defn inc-report-counter
  "Increments the named counter in *report-counters*, a ref to a map.
  Does nothing if *report-counters* is nil."
  {:added "1.1"}
  [name]
  (when *report-counters*
    (dosync (commute *report-counters* assoc name
                     (inc (or (*report-counters* name) 0))))))

;;; TEST RESULT REPORTING

(defmulti report :type)

(defmethod report :pass [m]
	   (inc-report-counter :pass))

(defmethod report :fail [m]
	   (inc-report-counter :fail)
	   (println "\nFAIL in" (file-position))
	   (println "expected:" (pr-str (:expected m)))
	   (println "  actual:" (pr-str (:actual m))))

(defmethod report :error [m]
	   (inc-report-counter :error)
	   (println "\nERROR in" (file-position))
	   (println "expected:" (pr-str (:expected m)))
	   (print "  actual: ")
	   (let [actual (:actual m)]
	     (if (instance? Throwable actual)
	       (stack/print-cause-trace actual nil)
	       (prn actual))))

(defmethod report :summary [m]
	   (println "\nRan" (:test m) "tests containing"
		    (+ (:pass m) (:fail m) (:error m)) "assertions.")
	   (println (:fail m) "failures," (:error m) "errors."))

;; Ignore these message types:
(defmethod report :begin-test-var [m])
(defmethod report :end-test-var [m])

;;; ASSERTION METHODS

(defmulti assert-expr (fn [form] (first form)))

(defmethod assert-expr :default [form]
	   ;;  "Returns generic assertion code for any functional predicate.  The
	   ;;  'expected' argument to 'report' will contains the original form, the
	   ;;  'actual' argument will contain the form with all its sub-forms
	   ;;  evaluated.  If the predicate returns false, the 'actual' form will
	   ;;  be wrapped in (not...)."
	   (let [args (rest form)
		 pred (first form)]
	     `(let [values# (list ~@args)
		    result# (apply ~pred values#)]
		(if result#
		  (report {:type :pass :expected '~form, :actual (cons ~pred values#)})
		  (report {:type :fail :expected '~form, :actual (list '~'not (cons '~pred values#))}))
		result#)))

(defmethod assert-expr 'thrown? [form]
	   ;; (is (thrown? c expr))
	   ;; Asserts that evaluating expr throws an exception of class c.
	   ;; Returns the exception thrown.
	   (let [klass (second form)
		 body (nthnext form 2)]
	     `(try ~@body
		   (report {:type :fail :expected '~form, :actual nil})
		   (catch ~klass e#
		     (report {:type :pass :expected '~form, :actual e#})
		     e#))))

;;; DEFINING TESTS

(defmacro is [form]
  `(try ~(assert-expr form)
        (catch Throwable t#
          (report {:type :error :expected '~form :actual t#}))))

(defmacro deftest
  "The test body goes in the :test metadata on the var,
  and the real function (the value of the var) calls test-var on
  itself."
  [& body]
  (let [name (gensym test)]
    `(def ~(vary-meta name assoc :test `(fn [] ~@body))
	  (fn [] (test-var (var ~name))))))

;;; RUNNING TESTS: LOW-LEVEL FUNCTIONS

(defn test-var [v]
  ;;;  "If v has a function in its :test metadata, calls that function,
  ;;;  with *testing-vars* bound to (conj *testing-vars* v)."
  (when-let [t (:test (meta v))]
    (report {:type :begin-test-var, :var v})
    (inc-report-counter :test)
    (t)
    (report {:type :end-test-var :var v})))

(defn test-all-vars [ns]
  (doseq [v (vals (ns-interns ns))]
    (when (:test (meta v))
      (test-var v))))

(defn test-ns [ns]
  (binding [*report-counters* (ref *initial-report-counters*)]
    (test-all-vars ns)
    @*report-counters*))

(defn run-tests [namespaces]
  (let [summary (assoc (apply merge-with + (map test-ns namespaces)) :type :summary)]
    (report summary)
    summary))

(defn run-all-tests
  ([] (run-tests (all-ns)))
  ([re] (run-tests (filter #(re-matches re (name (ns-name %))) (all-ns)))))

(-> (Runtime/getRuntime) (.addShutdownHook (Thread. run-all-tests)))

(defn which-comparison [expected actual options]
  (cond
   (and (:in options) (instance? java.util.Map expected)) :in-map
   (:in options) :in-set
   (instance? java.util.regex.Pattern expected) :regex
   (isa? expected Exception) :exception
   (= (class expected) Class) :class
   :default :equal))

(defmulti comparison which-comparison)

(defmethod comparison :equal [expected actual options] =)

(defmethod comparison :in-map [expected actual options]
	   (fn [e a] (= e (select-keys a (keys e)))))

(defmethod comparison :in-set [expected actual options] (fn [e a] (a e)))

(defmethod comparison :regex [expected actual options] re-seq)

(defmethod comparison :exception [expected actual options] 'thrown?)

(defmethod comparison :class [expected actual options] instance?)

(def in {:in true})

(defmacro expect 
  ([expected actual]
     `(deftest (is (~(comparison (eval expected) actual {}) ~expected ~actual))))
  ([expected option actual]
     `(deftest (is (~(comparison (eval expected) actual (eval option)) ~expected ~actual)))))
