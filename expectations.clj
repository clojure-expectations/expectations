(ns expectations
  (:use clojure.set)
  (:require clojure.template))

;;; GLOBALS
(def run-tests-on-shutdown (atom true))

(def *report-counters* nil)	  ; bound to a ref of a map in test-ns

(def *initial-report-counters*  ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0})

;;; UTILITIES FOR REPORTING FUNCTIONS

(defn stack->file&line [ex index]
  (let [s (nth (.getStackTrace ex) index)]
    (str (.getFileName s) ":" (.getLineNumber s))))

(defn inc-report-counter [name]
  (when *report-counters*
    (dosync (commute *report-counters* assoc name
                     (inc (or (*report-counters* name) 0))))))

;;; TEST RESULT REPORTING
(defn str-join [separator coll]
  (apply str (interpose separator (remove nil? coll))))

(defn test-name [{:keys [file line]}] (str (last (re-seq #"[A-Za-z_\.]+" file)) ":" line))

(defn raw-str [[e a]]
  (if (or (= ::true e) (= ":expectations/true" e))
    (str "(expect " a ")")
    (str "(expect " e " " a ")")))

(defn fail [file-pos msg] (println (str  "\nfailure in (" file-pos ")")) (println msg))
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
	   (fail (stack->file&line (new java.lang.Throwable) 4)
		 (str-join "\n"
			   [(when-let [msg (:raw m)]      (str         "      raw: " (raw-str msg)))
			    (when-let [msg (:result m)] (str           "   result: " (str-join " " msg)))
			    (when-let [msg (:expected-message m)] (str "  exp-msg: " msg))
			    (when-let [msg (:actual-message m)] (str   "  act-msg: " msg))
			    (when-let [msg (:message m)] (str          "  message: " msg))])))

(defmethod report :error [{:keys [result raw] :as m}]
	   (inc-report-counter :error)
	   (fail (stack->file&line result 4)
		 (str-join "\n"
			   [(when raw (str "      raw: " (raw-str raw)))
			    (when-let [msg (:expected-message m)] (str "  exp-msg: " msg))
			    (when-let [msg (:actual-message m)] (str   "  act-msg: " msg))
			    (str "    threw: " (class result) "-" (.getMessage result))
			    (str-join ""
				      (map (fn [{:keys [className methodName fileName lineNumber]}]
					     (str "           " className " (" fileName ":" lineNumber ")\n"))
					   (remove ignored-fns (map bean (.getStackTrace result)))))])))

(defmethod report :summary [m]
	   (summary (str "\nRan " (:test m) " tests containing "
			 (+ (:pass m) (:fail m) (:error m)) " assertions.\n"
			 (:fail m) " failures, " (:error m) " errors.")))

;; TEST RUNNING

(defn disable-run-on-shutdown [] (reset! run-tests-on-shutdown false))

(defn test-var [v]
  (when-let [t (var-get v)]
    (started (test-name (meta v)))
    (inc-report-counter :test)
    (t)
    (finished (test-name (meta v)))))

(defn test-all-vars [ns]
  (doseq [v (vals (ns-interns ns))]
    (when (:expectation (meta v))
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

(defmulti compare-expr (fn [e a str-e str-a]
			 (cond
			  (isa? e Throwable) ::expect-exception
			  (instance? Throwable e)  ::expected-exception
			  (instance? Throwable a)  ::actual-exception
			  (= ::true e) ::true
			  (::in-flag a) ::in
			  :default [(class e) (class a)])))

(defmethod compare-expr :default [e a str-e str-a]
	   (if (= e a)
	     (report {:type :pass})
	     (report {:type :fail :raw [str-e str-a]
		      :result [(pr-str e) "does not equal" (pr-str a)]})))

(defmethod compare-expr ::true [e a str-e str-a]
	   (if a
	     (report {:type :pass})
	     (report {:type :fail :raw [::true str-a]
		      :result [(pr-str a)]})))

(defmethod compare-expr ::in [e a str-e str-a]
	   (cond
	    (instance? java.util.List (::in a))
	    (if (seq (filter (fn [item] (= e item)) (::in a)))
	      (report {:type :pass})
	      (report {:type :fail :raw [str-e str-a]
		       :result ["value" (pr-str e) "not found in" (::in a)]}))
	    (instance? java.util.Set (::in a))
	    (if ((::in a) e)
	      (report {:type :pass})
	      (report {:type :fail :raw [str-e str-a]
		       :result ["key" (pr-str e) "not found in" (::in a)]}))
	    (instance? java.util.Map (::in a))
	    (let [sub-a (select-keys (::in a) (keys e))] 
	      (if (= e sub-a)
		(report {:type :pass})
		(let [in-both (intersection (set (keys e)) (set (keys sub-a)))
		      in-both-map (select-keys (merge-with vector e sub-a) in-both)
		      disagreeing (filter (fn [[x [y z]]] (not= y z)) in-both-map)
		      format-fn (fn [[x [y z]]] (str (pr-str x) " expected " (pr-str y) " but was " (pr-str z)))
		      messages (seq (map format-fn disagreeing))
		      diff-fn (fn [x y] (seq (difference (set (keys x)) (set (keys y)))))]
		  (report {:type :fail
			   :actual-message (when-let [v (diff-fn e sub-a)]
					     (str (str-join ", " v) " are in expected, but not in actual"))
			   :raw [str-e str-a]
			   :result [e "are not in" (::in a)]
			   :message (when messages (str-join ", " messages))}))))
	    :default (report {:type :fail :raw [str-e str-a]
			      :result [(pr-str (::in a))]
			      :message "You must supply a list, set, or map when using (in)"})))

(defmethod compare-expr [Class Object] [e a str-e str-a]
	   (if (instance? e a)
	     (report {:type :pass})
	     (report {:type :fail :raw [str-e str-a]
		      :result [a "is not an instance of" e]})))


(defmethod compare-expr ::actual-exception [e a str-e str-a]
	   (report {:type :error
		    :raw [str-e str-a]
		    :actual-message (str "exception in actual: " str-a)
		    :result a}))

(defmethod compare-expr ::expected-exception [e a str-e str-a]
	   (report {:type :error
		    :raw [str-e str-a]
		    :expected-message (str "exception in expected: " str-e)
		    :result e}))

(defmethod compare-expr [java.util.regex.Pattern Object] [e a str-e str-a]
	   (if (re-seq e a)
	     (report {:type :pass})
	     (report {:type :fail,
		      :raw [str-e str-a]
		      :result ["regex" (pr-str e) "not found in" (pr-str a)]})))

(defmethod compare-expr ::expect-exception [e a str-e str-a]
	   (if (instance? e a)
	     (report {:type :pass})
	     (report {:type :fail :raw [str-e str-a]
		      :result [str-a "did not throw" str-e]})))

(defmethod compare-expr [java.util.Map java.util.Map] [e a str-e str-a]
	   (if (= e a)
	     (report {:type :pass})
	     (let [in-both (intersection (set (keys e)) (set (keys a)))
		   in-both-map (select-keys (merge-with vector e a) in-both)
		   disagreeing (filter (fn [[x [y z]]] (not= y z)) in-both-map)
		   format-fn (fn [[x [y z]]] (str (pr-str x) " expected " (pr-str y) " but was " (pr-str z)))
		   messages (seq (map format-fn disagreeing))
		   diff-fn (fn [e a] (seq (difference (set (keys e)) (set (keys a)))))]
	       (report {:type :fail
			:actual-message (when-let [v (diff-fn e a)]
					  (str (str-join ", " v) " are in expected, but not in actual"))
			:expected-message (when-let [v (diff-fn a e)]
					    (str (str-join ", " v) " are in actual, but not in expected"))
			:raw [str-e str-a]
			:result [e "does not equal" a]
			:message (when messages (str-join ", " messages))}))))

(defmethod compare-expr [java.util.Set java.util.Set] [e a str-e str-a]
	   (if (= e a)
	     (report {:type :pass})
	     (let [diff-fn (fn [e a] (seq (difference e a)))]
	       (report {:type :fail
			:actual-message (when-let [v (diff-fn e a)]
					  (str (str-join ", " v) " are in expected, but not in actual"))
			:expected-message (when-let [v (diff-fn a e)]
					    (str (str-join ", " v) " are in actual, but not in expected"))
			:raw [str-e str-a]
			:result [e "does not equal" a]}))))

(defmethod compare-expr [java.util.List java.util.List] [e a str-e str-a]
	   (if (= e a)
	     (report {:type :pass})
	     (let [diff-fn (fn [e a] (seq (difference (set e) (set a))))]
	       (report {:type :fail
			:actual-message (when-let [v (diff-fn e a)]
					  (str (str-join ", " v) " are in expected, but not in actual"))
			:expected-message (when-let [v (diff-fn a e)]
					    (str (str-join ", " v) " are in actual, but not in expected"))
			:raw [str-e str-a]
			:result [e "does not equal" a]
			:message (cond
				  (and
				   (= (set e) (set a))
				   (= (count e) (count a))
				   (= (count e) (count (set a))))
				  "lists appears to contain the same items with different ordering"
				  (and (= (set e) (set a)) (< (count e) (count a)))
				  "some duplicate items in actual are not expected"
				  (and (= (set e) (set a)) (> (count e) (count a)))
				  "some duplicate items in expected are not actual"
				  (< (count e) (count a))
				  "actual is larger than expected"
				  (> (count e) (count a))
				  "expected is larger than actual")

			}))))

(defmacro doexpect [e a]
  `(let [e# (try ~e (catch Throwable t# t#))
	 a# (try ~a (catch Throwable t# t#))]
     (compare-expr e# a# ~(str e) ~(str a))))

(defmacro check
  ([e a] `(binding [fail (fn [_# msg#] (throw (AssertionError. msg#)))]
	    (doexpect ~e ~a)))
  ([a] `(binding [fail (fn [_# msg#] (throw (AssertionError. msg#)))]
	    (doexpect ::true ~a))))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym "test") assoc :expectation true)
	(fn []
	  (try ~@forms
	       (catch AssertionError e#
		 (fail (stack->file&line e# 5) (.getMessage e#)))
	       (catch Throwable t#
		 (report {:type :error :result t#}))))))

(defmacro expect
  ([e a]
     `(def ~(vary-meta (gensym "test") assoc :expectation true)
	   (fn [] (doexpect ~e ~a))))
  ([a]
     `(def ~(vary-meta (gensym "test") assoc :expectation true)
	   (fn [] (doexpect ::true ~a)))))

(defmacro given
  ([bindings form & args]
     `(clojure.template/do-template ~bindings ~form ~@args)))

(defn in [n] {::in n ::in-flag true})

(->
 (Runtime/getRuntime)
 (.addShutdownHook
  (proxy [Thread] []
    (run [] (when @run-tests-on-shutdown (run-all-tests))))))