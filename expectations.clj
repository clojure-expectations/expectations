(ns expectations
  (:require [clojure.stacktrace :as stack]))

;;; GLOBALS USED BY THE REPORTING FUNCTIONS

(def *report-counters* nil)	  ; bound to a ref of a map in test-ns

(def *initial-report-counters*  ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0})

;;; UTILITIES FOR REPORTING FUNCTIONS

(defn file-position []
;  (doseq [x (.getStackTrace (new java.lang.Throwable))] (println x))
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
	   (when-let [msg (:expected m)] (println          "      raw:" msg))
	   (when-let [msg (:actual m)] (println            "evaluated:" msg))
	   (when-let [msg (:expected-message m)] (println  "  exp-msg:" msg))
	   (when-let [msg (:actual-message m)] (println    "  act-msg:" msg))
	   (when-let [msg (:message m)] (println           "  message:" msg))
	   (println))

(defmethod report :error [m]
	   (inc-report-counter :error)
	   (println "\nERROR in" (file-position))
	   (when-let [msg (:message m)] (println msg))
	   (println "expected:" (pr-str (:expected m)))
	   (print "  actual: ")
	   (let [actual (:actual m)]
	     (if (instance? Throwable actual)
	       (stack/print-cause-trace actual 1)
	       (prn actual))))

(defmethod report :summary [m]
	   (println "\nRan" (:test m) "tests containing"
		    (+ (:pass m) (:fail m) (:error m)) "assertions.")
	   (println (:fail m) "failures," (:error m) "errors."))

;; Ignore these message types:
(defmethod report :begin-test-var [m])
(defmethod report :end-test-var [m])

;;; ASSERTION METHODS

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


(defn str-join [separator coll]
  (apply str (interpose separator coll)))

(defmacro expect-in-map [e a o]
  `(def ~(with-meta (gensym "test") {:test true})
	(fn []
	  (let [expected# (eval ~e)
		actual# (eval ~a)]
	    (if (= expected# (select-keys actual# (keys expected#)))
	      (report {:type :pass})
	      (report {:type :fail
		       :file-position (file-position)
		       :expected (str ~e " expected in " ~a)
		       :actual (str expected# " was not found in " actual#)}))))))

(defmacro expect-equal-map [e a o]
  `(def ~(with-meta (gensym "test") {:test true})
	(fn []
	  (let [expected# (eval ~e)
		actual# (eval ~a)]
	    (if (= expected# actual#)
	      (report {:type :pass})
	      (let [expected-nf# (keys (apply dissoc actual# (keys expected#)))
		    actual-nf# (keys (apply dissoc expected# (keys actual#)))
		    in-both# (merge-with vector
					 (apply dissoc expected# actual-nf#)
					 (apply dissoc actual# expected-nf#))
		    disagreeing# (filter (fn [[x# [y# z#]]] (not= y# z#)) in-both#)]
		(report {:type :fail
			 :actual-message (when actual-nf#
					   (str actual-nf# " are in expected, but not in actual"))
			 :expected-message (when expected-nf#
					     (str expected-nf# " are in actual, but not in expected"))
			 :message (when (seq disagreeing#)
				    (str-join ", "
					      (map
					       (fn [[key# [exp# act#]]]
						 (str key# " expected " exp# " but was " act#))
					       disagreeing#)))
			 :expected (str ~e " expected in " ~a)
			 :actual (str expected# " was not found in " actual#)})))))))

(defmacro expect-in-set [e a o]
  `(def ~(with-meta (gensym "test") {:test true})
	(fn []
	  (if ((eval ~e) (eval ~a))
	    (report {:type :pass})
	    (report {:type :fail
		     :expected (str ~e " in " ~a)
		     :actual (str (eval ~e) " not found in " (eval ~a))})))))


(-> (Runtime/getRuntime) (.addShutdownHook (Thread. run-all-tests)))

(defmulti assert-expr (fn [& _] ))

(defmethod assert-expr :default [e a]
	   `(let [values# (list ~e ~a)
		  result# (apply = values#)]
	      (if result#
		(report {:type :pass})
		(report {:type :fail,
			 :expected (list '~ 'expect '~e '~a),
			 :actual (list '~ 'not (cons '~ '= values#))}))))

(defmacro doexpect [e a]
  `(try ~(assert-expr e a)
	(catch Throwable t#
	  (report {:type :error, :expected (list '~ 'expect '~e '~a), :actual t#}))))

(defmacro expect [e a]
  `(def ~(vary-meta (gensym "test") assoc :test true)
	(fn [] (doexpect ~e ~a))))

