(ns expectations
  (:require [clojure.stacktrace :as stack]))

;;; GLOBALS USED BY THE REPORTING FUNCTIONS

(def *report-counters* nil)	  ; bound to a ref of a map in test-ns

(def *initial-report-counters*  ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0})

;;; UTILITIES FOR REPORTING FUNCTIONS

(defn file-position []
  (let [^StackTraceElement s (nth (.getStackTrace (new java.lang.Throwable)) 3)]
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
	   (when-let [msg (:message m)] (println msg))
	   (when-let [msg (:expected m)] (println "expected:" msg))
	   (when-let [msg (:actual m)] (println "  actual:" msg))
	   (println))

(defmethod report :error [m]
	   (inc-report-counter :error)
	   (println "\nERROR in" (file-position))
	   (when-let [msg (:message m)] (println msg))
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
(defmethod report :begin-test-var [m] (println m))
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
		  (report {:type :pass})
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

;;; RUNNING TESTS: LOW-LEVEL FUNCTIONS

(defn test-var [v]
  ;;;  "If v has a function in its :test metadata, calls that function,
  ;;;  with *testing-vars* bound to (conj *testing-vars* v)."
  (when-let [t (var-get v)]
    (report {:type :begin-test-var, :var v})
    (inc-report-counter :test)
    (t)
    (report {:type :end-test-var :var v})))

(defn test-all-vars [ns]
  (doseq [v (vals (ns-interns ns))]
;    (println v (meta v))
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

(defmethod comparison :in-map [expected actual options] (fn [e a] (= e (select-keys a (keys e)))))

(defmethod comparison :in-set [expected actual options] (fn [e a] (a e)))

(defmethod comparison :regex [expected actual options] re-seq)

(defmethod comparison :exception [expected actual options] 'thrown?)

(defmethod comparison :class [expected actual options] instance?)

(defmacro defexpect [body]
  (let [n (gensym test)]
    `(def ~(vary-meta n assoc :test true :test2
		      `(fn []
			 (try ~(assert-expr body)
			      (catch Throwable t#
				(report {:type :error :expected '~body :actual t#})))))
	  (fn [] ((:test2 (meta (var ~n))))))))

(defmacro expect-in-map [e a]
  `(def ~(with-meta (gensym "test") {:test true})
	(fn []
	  (if (= (eval ~e) (select-keys (eval ~a) (keys (eval ~e))))
	    (report {:type :pass})
	    (report {:type :fail
		     :expected (str ~e " in " ~a)
		     :actual (str (eval ~e) " not found in " (eval ~a))})))))

(defmacro expect-in-set [e a]
  `(def ~(with-meta (gensym "test") {:test true})
	(fn []
	  (if ((eval ~e) (eval ~a))
	    (report {:type :pass})
	    (report {:type :fail
		     :expected (str ~e " in " ~a)
		     :actual (str (eval ~e) " not found in " (eval ~a))})))))

(defmacro defexpect2 [e a o]
  (cond
   (and (:in o) (instance? java.util.Map (eval e))) (expect-in-map e a)
   (:in o) (expect-in-set e a)))

(defmacro expect 
  ([expected actual]
     `(defexpect (~(comparison (eval expected) actual {}) ~expected ~actual)))
  ([expected _ actual]
     `(defexpect2 ~expected ~actual {:in true})))

