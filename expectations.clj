(ns expectations
  (require clojure.template))

;;; GLOBALS
(def run-tests-on-shutdown (atom true))

(def *report-counters* nil)	  ; bound to a ref of a map in test-ns

(def *initial-report-counters*  ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0})

;;; UTILITIES FOR REPORTING FUNCTIONS

(defn file-position []
  (let [s (nth (.getStackTrace (new java.lang.Throwable)) 3)]
    (str (.getFileName s) ":" (.getLineNumber s))))

(defn inc-report-counter [name]
  (when *report-counters*
    (dosync (commute *report-counters* assoc name
                     (inc (or (*report-counters* name) 0))))))

;;; TEST RESULT REPORTING
(defn str-join [separator coll]
  (apply str (interpose separator (remove nil? coll))))

(defn test-name [{:keys [file line]}] (str (last (re-seq #"[A-Za-z_\.]+" file)) ":" line))

(defn fail [_ msg] (println msg))
(defn summary [msg] (println msg))
(defn started [test-name])
(defn finished [test-name])

(defn ignored-fns [{:keys [className fileName]}]
  (or (= fileName "expectations.clj")
      (re-seq #"clojure.lang" className)
      (re-seq #"clojure.core" className)
      (re-seq #"clojure.main" className)
      (re-seq #"java.lang" className)))

(defmulti report :type)

(defmethod report :pass [m]
	   (inc-report-counter :pass))

(defmethod report :fail [m]
	   (inc-report-counter :fail)
	   (fail (file-position)
		 (str-join "\n"
			   [(str "\nFAIL in (" (file-position) ")")
			    (when-let [msg (:expected m)] (str         "      raw: " msg))
			    (when-let [msg (:actual m)] (str           "   result: " msg))
			    (when-let [msg (:expected-message m)] (str "  exp-msg: " msg))
			    (when-let [msg (:actual-message m)] (str   "  act-msg: " msg))
			    (when-let [msg (:message m)] (str          "  message: " msg))])))

(defmethod report :error [{:keys [actual expected]}]
	   (inc-report-counter :error)
	   (fail (file-position)
		 (str-join "\n"
			[(str "\nERROR in (" (file-position) ")")
			 (str "      raw:" (pr-str expected))
			 (str "    threw: " (class actual) "-" (.getMessage actual))
			 (str-join ""
				   (map (fn [{:keys [className methodName fileName lineNumber]}]
					  (str "    " className "." methodName " (" fileName ":" lineNumber ")\n"))
					(remove ignored-fns (map bean (.getStackTrace actual)))))])))

(defmethod report :summary [m]
	   (summary (str "\nRan " (:test m) " tests containing "
			 (+ (:pass m) (:fail m) (:error m)) " assertions.\n"
			 (:fail m) " failures, " (:error m) " errors.")))

(defmethod report :begin-test-var [m]
	   (started (test-name (meta (:var m)))))

(defmethod report :end-test-var [m]
	   (finished (test-name (meta (:var m)))))

;; TEST RUNNING

(defn disable-run-on-shutdown [] (reset! run-tests-on-shutdown false))

(defn test-var [v]
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

(defmulti assert-expr
  (fn [e a]
    (let [expected (try (eval e)
			(catch Throwable t
			  (throw (RuntimeException. "the expected value cannot throw an exception" t))))
	  actual (try (eval a)
		      (catch Throwable t nil))]
      (cond
       (isa? expected Throwable) expected
       (::in actual) ::in
       (::is actual) ::is
       :default [(class expected) (class actual)]))))


(defmethod assert-expr ::in [e a]
	   `(cond
	     (instance? java.util.List (::in ~a))
	     (if (seq (filter (fn [item#] (= ~e item#)) (::in ~a)))
	       (report {:type :pass})
	       (report {:type :fail,
			:expected (list '~ 'expect '~e '~a),
			:actual (str "value " ~e " not found in " (::in ~a))}))
	     (instance? java.util.Set (::in ~a))
	     (if (~e (::in ~a))
	       (report {:type :pass})
	       (report {:type :fail,
			:expected (list '~ 'expect '~e '~a),
			:actual (str "key " ~e " not found in " (::in ~a))}))
	     (instance? java.util.Map (::in ~a))
	     (let [sub-a# (select-keys (::in ~a) (keys ~e))] 
	       (if (= ~e sub-a#)
		 (report {:type :pass})
		 (let [actual-nf# (keys (apply dissoc ~e (keys sub-a#)))
		       in-both# (merge-with vector (select-keys ~e (keys sub-a#)) sub-a#)
		       disagreeing# (filter (fn [[x# [y# z#]]] (not= y# z#)) in-both#)]
		   (report {:type :fail
			    :actual-message (when actual-nf#
					      (str actual-nf# " are in expected, but not in actual"))
			    :expected (list '~ 'expect '~e '~a)
			    :actual (str-join " " [~e "are not in" (::in ~a)])
			    :message (when (seq disagreeing#)
				       (str-join ", "
						 (map
						  (fn [[key# [exp# act#]]]
						    (str key# " expected " exp# " but was " act#))
						  disagreeing#)))
			    }))))
	     :default (report {:type :fail,
			       :expected (list '~ 'expect '~e '~a),
			       :actual "You must supply a set or map when using (in ,,,)"})))

(defmethod assert-expr ::is [e a]
	   `(if ((::is ~a) ~e)
	      (report {:type :pass})
	      (report {:type :fail,
		       :expected (list '~ 'expect '~e '~a),
		       :actual (str ~e " is not " (last '~a))})))

(defmethod assert-expr [java.util.regex.Pattern Object] [e a]
	   `(if (re-seq ~e ~a)
	      (report {:type :pass})
	      (report {:type :fail,
		       :expected (list '~ 'expect '~e '~a),
		       :actual (str "regex #\"" ~e "\" not found in \"" ~a "\"")})))

(defmethod assert-expr Exception [e a]
	   `(try ~a
		 (report {:type :fail :expected (list '~ 'expect '~e '~a) :actual (str-join " " ['~a "did not throw" '~e])})
		 (catch ~e e#
		   (report {:type :pass}))))

(defmethod assert-expr [Class Object] [e a]
	   `(if (instance? ~e ~a)
	      (report {:type :pass})
	      (report {:type :fail,
		       :expected (list '~ 'expect '~e '~a),
		       :actual (str-join " " ['~a "is not an instance of" '~e])})))

(defmethod assert-expr [java.util.Map java.util.Map] [e a]
	   `(if (= ~e ~a)
	      (report {:type :pass})
	      (let [expected-nf# (keys (apply dissoc ~a (keys ~e)))
		    actual-nf# (keys (apply dissoc ~e (keys ~a)))
		    in-both# (merge-with vector
					 (apply dissoc ~e actual-nf#)
					 (apply dissoc ~a expected-nf#))
		    disagreeing# (filter (fn [[x# [y# z#]]] (not= y# z#)) in-both#)]
		(report {:type :fail
			 :actual-message (when actual-nf#
					   (str actual-nf# " are in expected, but not in actual"))
			 :expected-message (when expected-nf#
					     (str expected-nf# " are in actual, but not in expected"))
			 :expected (list '~ 'expect '~e '~a),
			 :actual (str-join " " [~e "does not equal" ~a])
			 :message (when (seq disagreeing#)
				    (str-join ", "
					      (map
					       (fn [[key# [exp# act#]]]
						 (str key# " expected " exp# " but was " act#))
					       disagreeing#)))}))))

(defmethod assert-expr :default [e a]
	   `(if (= ~e ~a)
	      (report {:type :pass})
	      (report {:type :fail,
		       :expected (list '~ 'expect '~e '~a),
		       :actual (str-join " " [~e "does not equal" ~a])})))

(defmacro doexpect [e a]
  `(try ~(assert-expr e a)
	(catch Throwable t#
	  (report {:type :error, :expected (list '~ 'expect '~e '~a), :actual t#}))))

(defmacro expect
  ([e a]
     `(def ~(vary-meta (gensym "test") assoc :test true)
	   (fn [] (doexpect ~e ~a))))
  ([bindings e a & args]
     `(clojure.template/do-template ~bindings (expect ~e ~a) ~@args)))

(defn in [n] {::in n})
(defn is [n] {::is n})

(->
 (Runtime/getRuntime)
 (.addShutdownHook
  (proxy [Thread] []
    (run [] (when @run-tests-on-shutdown (run-all-tests))))))