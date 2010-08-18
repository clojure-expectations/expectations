(ns expectations
  (:require
   [clojure.stacktrace :as stack]
   [clojure.template :as temp]))

(defonce
  ^{:doc "True by default.  If set to false, no test functions will
   be created by deftest, or with-test.  Use this to omit
   tests when compiling or loading production code."
    :added "1.1"}
  *load-tests* true)

(def
 ^{:doc "The maximum depth of stack traces to print when an Exception
  is thrown during a test.  Defaults to nil, which means print the 
  complete stack trace."
   :added "1.1"}
 *stack-trace-depth* nil)


;;; GLOBALS USED BY THE REPORTING FUNCTIONS

(def *report-counters* nil)	  ; bound to a ref of a map in test-ns

(def *initial-report-counters*  ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0})

(def *testing-vars* (list))  ; bound to hierarchy of vars being tested

(def *testing-contexts* (list)) ; bound to hierarchy of "testing" strings

(def *test-out* *out*)         ; PrintWriter for test reporting output

(defmacro with-test-out
  "Runs body with *out* bound to the value of *test-out*."
  {:added "1.1"}
  [& body]
  `(binding [*out* *test-out*]
     ~@body))



;;; UTILITIES FOR REPORTING FUNCTIONS

(defn file-position
  "Returns a vector [filename line-number] for the nth call up the
  stack."
  {:added "1.1"}
  [n]
  (let [^StackTraceElement s (nth (.getStackTrace (new java.lang.Throwable)) n)]
    [(.getFileName s) (.getLineNumber s)]))

(defn testing-vars-str
  "Returns a string representation of the current test.  Renders names
  in *testing-vars* as a list, then the source file and line of
  current assertion."
  {:added "1.1"}
  []
  (let [[file line] (file-position 4)]
    (str
     ;; Uncomment to include namespace in failure report:
     ;;(ns-name (:ns (meta (first *testing-vars*)))) "/ "
     (reverse (map #(:name (meta %)) *testing-vars*))
     " (" file ":" line ")")))

(defn testing-contexts-str
  "Returns a string representation of the current test context. Joins
  strings in *testing-contexts* with spaces."
  {:added "1.1"}
  []
  (apply str (interpose " " (reverse *testing-contexts*))))

(defn inc-report-counter
  "Increments the named counter in *report-counters*, a ref to a map.
  Does nothing if *report-counters* is nil."
  {:added "1.1"}
  [name]
  (when *report-counters*
    (dosync (commute *report-counters* assoc name
                     (inc (or (*report-counters* name) 0))))))



;;; TEST RESULT REPORTING

(defmulti
  ^{:doc "Generic reporting function, may be overridden to plug in
   different report formats (e.g., TAP, JUnit).  Assertions such as
   'is' call 'report' to indicate results.  The argument given to
   'report' will be a map with a :type key.  See the documentation at
   the top of test_is.clj for more information on the types of
   arguments for 'report'."
    :dynamic true
    :added "1.1"}
  report :type)

(defmethod report :default [m]
	   (with-test-out (prn m)))

(defmethod report :pass [m]
	   (with-test-out (inc-report-counter :pass)))

(defmethod report :fail [m]
	   (with-test-out
	     (inc-report-counter :fail)
	     (println "\nFAIL in" (testing-vars-str))
	     (when (seq *testing-contexts*) (println (testing-contexts-str)))
	     (when-let [message (:message m)] (println message))
	     (println "expected:" (pr-str (:expected m)))
	     (println "  actual:" (pr-str (:actual m)))))

(defmethod report :error [m]
	   (with-test-out
	     (inc-report-counter :error)
	     (println "\nERROR in" (testing-vars-str))
	     (when (seq *testing-contexts*) (println (testing-contexts-str)))
	     (when-let [message (:message m)] (println message))
	     (println "expected:" (pr-str (:expected m)))
	     (print "  actual: ")
	     (let [actual (:actual m)]
	       (if (instance? Throwable actual)
		 (stack/print-cause-trace actual *stack-trace-depth*)
		 (prn actual)))))

(defmethod report :summary [m]
	   (with-test-out
	     (println "\nRan" (:test m) "tests containing"
		      (+ (:pass m) (:fail m) (:error m)) "assertions.")
	     (println (:fail m) "failures," (:error m) "errors.")))

;; Ignore these message types:
(defmethod report :begin-test-var [m])
(defmethod report :end-test-var [m])



;;; UTILITIES FOR ASSERTIONS

(defn get-possibly-unbound-var
  "Like var-get but returns nil if the var is unbound."
  {:added "1.1"}
  [v]
  (try (var-get v)
       (catch IllegalStateException e
         nil)))

(defn function?
  "Returns true if argument is a function or a symbol that resolves to
  a function (not a macro)."
  {:added "1.1"}
  [x]
  (if (symbol? x)
    (when-let [v (resolve x)]
      (when-let [value (get-possibly-unbound-var v)]
        (and (fn? value)
             (not (:macro (meta v))))))
    (fn? x)))

(defn assert-predicate
  "Returns generic assertion code for any functional predicate.  The
  'expected' argument to 'report' will contains the original form, the
  'actual' argument will contain the form with all its sub-forms
  evaluated.  If the predicate returns false, the 'actual' form will
  be wrapped in (not...)."
  {:added "1.1"}
  [msg form]
  (let [args (rest form)
        pred (first form)]
    `(let [values# (list ~@args)
           result# (apply ~pred values#)]
       (if result#
         (report {:type :pass, :message ~msg,
                  :expected '~form, :actual (cons ~pred values#)})
         (report {:type :fail, :message ~msg,
                  :expected '~form, :actual (list '~'not (cons '~pred values#))}))
       result#)))

(defn assert-any
  "Returns generic assertion code for any test, including macros, Java
  method calls, or isolated symbols."
  {:added "1.1"}
  [msg form]
  `(let [value# ~form]
     (if value#
       (report {:type :pass, :message ~msg,
                :expected '~form, :actual value#})
       (report {:type :fail, :message ~msg,
                :expected '~form, :actual value#}))
     value#))



;;; ASSERTION METHODS

;; You don't call these, but you can add methods to extend the 'is'
;; macro.  These define different kinds of tests, based on the first
;; symbol in the test expression.

(defmulti assert-expr 
  (fn [msg form]
    (cond
     (nil? form) :always-fail
     (seq? form) (first form)
     :else :default)))

(defmethod assert-expr :always-fail [msg form]
	   ;; nil test: always fail
	   `(report {:type :fail, :message ~msg}))

(defmethod assert-expr :default [msg form]
	   (if (and (sequential? form) (function? (first form)))
	     (assert-predicate msg form)
	     (assert-any msg form)))

(defmethod assert-expr 'instance? [msg form]
	   ;; Test if x is an instance of y.
	   `(let [klass# ~(nth form 1)
		  object# ~(nth form 2)]
	      (let [result# (instance? klass# object#)]
		(if result#
		  (report {:type :pass, :message ~msg,
			   :expected '~form, :actual (class object#)})
		  (report {:type :fail, :message ~msg,
			   :expected '~form, :actual (class object#)}))
		result#)))

(defmethod assert-expr 'thrown? [msg form]
	   ;; (is (thrown? c expr))
	   ;; Asserts that evaluating expr throws an exception of class c.
	   ;; Returns the exception thrown.
	   (let [klass (second form)
		 body (nthnext form 2)]
	     `(try ~@body
		   (report {:type :fail, :message ~msg,
			    :expected '~form, :actual nil})
		   (catch ~klass e#
		     (report {:type :pass, :message ~msg,
			      :expected '~form, :actual e#})
		     e#))))

(defmethod assert-expr 'thrown-with-msg? [msg form]
	   ;; (is (thrown-with-msg? c re expr))
	   ;; Asserts that evaluating expr throws an exception of class c.
	   ;; Also asserts that the message string of the exception matches
	   ;; (with re-find) the regular expression re.
	   (let [klass (nth form 1)
		 re (nth form 2)
		 body (nthnext form 3)]
	     `(try ~@body
		   (report {:type :fail, :message ~msg, :expected '~form, :actual nil})
		   (catch ~klass e#
		     (let [m# (.getMessage e#)]
		       (if (re-find ~re m#)
			 (report {:type :pass, :message ~msg,
				  :expected '~form, :actual e#})
			 (report {:type :fail, :message ~msg,
				  :expected '~form, :actual e#})))
		     e#))))


(defmacro try-expr
  "Used by the 'is' macro to catch unexpected exceptions.
  You don't call this."
  {:added "1.1"}
  [msg form]
  `(try ~(assert-expr msg form)
        (catch Throwable t#
          (report {:type :error, :message ~msg,
                   :expected '~form, :actual t#}))))



;;; ASSERTION MACROS

;; You use these in your tests.

(defmacro is
  "Generic assertion macro.  'form' is any predicate test.
  'msg' is an optional message to attach to the assertion.
  
  Example: (is (= 4 (+ 2 2)) \"Two plus two should be 4\")

  Special forms:

  (is (thrown? c body)) checks that an instance of c is thrown from
  body, fails if not; then returns the thing thrown.

  (is (thrown-with-msg? c re body)) checks that an instance of c is
  thrown AND that the message on the exception matches (with
  re-find) the regular expression re."
  {:added "1.1"} 
  ([form] `(is ~form nil))
  ([form msg] `(try-expr ~msg ~form)))

(defmacro are
  "Checks multiple assertions with a template expression.
  See clojure.template/do-template for an explanation of
  templates.

  Example: (are [x y] (= x y)  
                2 (+ 1 1)
                4 (* 2 2))
  Expands to: 
           (do (is (= 2 (+ 1 1)))
               (is (= 4 (* 2 2))))

  Note: This breaks some reporting features, such as line numbers."
  {:added "1.1"}
  [argv expr & args]
  `(temp/do-template ~argv (is ~expr) ~@args))

(defmacro testing
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [string & body]
  `(binding [*testing-contexts* (conj *testing-contexts* ~string)]
     ~@body))



;;; DEFINING TESTS

(defmacro with-test
  "Takes any definition form (that returns a Var) as the first argument.
  Remaining body goes in the :test metadata function for that Var.

  When *load-tests* is false, only evaluates the definition, ignoring
  the tests."
  {:added "1.1"}
  [definition & body]
  (if *load-tests*
    `(doto ~definition (alter-meta! assoc :test (fn [] ~@body)))
    definition))


(defmacro deftest
  "Defines a test function with no arguments.  Test functions may call
  other tests, so tests may be composed.  If you compose tests, you
  should also define a function named test-ns-hook; run-tests will
  call test-ns-hook instead of testing all vars.

  Note: Actually, the test body goes in the :test metadata on the var,
  and the real function (the value of the var) calls test-var on
  itself.

  When *load-tests* is false, deftest is ignored."
  {:added "1.1"}
  [name & body]
  (when *load-tests*
    `(def ~(vary-meta name assoc :test `(fn [] ~@body))
          (fn [] (test-var (var ~name))))))

(defmacro deftest-
  "Like deftest but creates a private var."
  {:added "1.1"}
  [name & body]
  (when *load-tests*
    `(def ~(vary-meta name assoc :test `(fn [] ~@body) :private true)
          (fn [] (test-var (var ~name))))))

;;; RUNNING TESTS: LOW-LEVEL FUNCTIONS

(defn test-var [v]
  ;;;  "If v has a function in its :test metadata, calls that function,
  ;;;  with *testing-vars* bound to (conj *testing-vars* v)."
  (when-let [t (:test (meta v))]
    (binding [*testing-vars* (conj *testing-vars* v)]
      (report {:type :begin-test-var, :var v})
      (inc-report-counter :test)
      (try (t)
           (catch Throwable e
             (report {:type :error, :message "Uncaught exception, not in assertion." :expected nil, :actual e})))
      (report {:type :end-test-var, :var v}))))

(defn test-all-vars [ns]
  (doseq [v (vals (ns-interns ns))]
    (when (:test (meta v))
      (test-var v))))

(defn test-ns [ns]
  (binding [*report-counters* (ref *initial-report-counters*)]
    (test-all-vars ns)
    @*report-counters*))

(defn run-tests [& namespaces]
  (let [summary (assoc (apply merge-with + (map test-ns namespaces)) :type :summary)]
    (report summary)
    summary))

(defn run-all-tests
  ([] (apply run-tests (all-ns)))
  ([re] (apply run-tests (filter #(re-matches re (name (ns-name %))) (all-ns)))))

(defn successful?
  "Returns true if the given test summary indicates all tests
  were successful, false otherwise."
  {:added "1.1"}
  [summary]
  (and (zero? (:fail summary 0))
       (zero? (:error summary 0))))

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
     `(deftest ~(gensym)
	(is (~(comparison (eval expected) actual {}) ~expected ~actual))))
  ([expected option actual]
     `(deftest ~(gensym)
	(is (~(comparison (eval expected) actual (eval option)) ~expected ~actual)))))
