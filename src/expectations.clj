(ns expectations
  (:use clojure.set)
  (:require clojure.template))

;;; GLOBALS
(def run-tests-on-shutdown (atom true))

(def *test-name* "test name unset")
(def *test-meta* {})
(def *prune-stacktrace* true)

(def *report-counters* nil) ; bound to a ref of a map in test-ns

(def *initial-report-counters* ; used to initialize *report-counters*
     {:test 0, :pass 0, :fail 0, :error 0 :run-time 0})

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

(defn test-name [{:keys [line ns]}]
  (str ns ":" line))

(defn test-file [{:keys [file line]}]
  (str (last (re-seq #"[A-Za-z_\.]+" file)) ":" line))

(defn raw-str [[e a]]
  (str "(expect " e (when (> (count e) 30) "\n                  ") " " a ")"))

(defn fail [test-name test-meta msg] (println (str "\nfailure in (" (test-file test-meta) ") : " (:ns test-meta))) (println msg))
(defn summary [msg] (println msg))
(defn started [test-name test-meta])
(defn finished [test-name test-meta])

(defn ignored-fns [{:keys [className fileName]}]
  (when *prune-stacktrace*
    (or (= fileName "expectations.clj")
	(re-seq #"clojure.lang" className)
	(re-seq #"clojure.core" className)
	(re-seq #"clojure.main" className)
	(re-seq #"java.lang" className))))

(defn pruned-stack-trace [t]
  (str-join "\n"
	    (distinct (map (fn [{:keys [className methodName fileName lineNumber] :as m}]
			     (if (= methodName "invoke")
			       (str "           on (" fileName ":" lineNumber ")")
			       (str "           " className "$" methodName " (" fileName ":" lineNumber ")")))
			   (remove ignored-fns (map bean (.getStackTrace t)))))))

(defmulti report :type)

(defmethod report :pass [m]
	   (inc-report-counter :pass))

(defmethod report :fail [m]
	   (inc-report-counter :fail)
	   (fail *test-name* *test-meta*
		 (str-join "\n"
			   [(when-let [msg (:raw m)] (str "           " (raw-str msg)))
			    (when-let [msg (:result m)] (str "           " (str-join " " msg)))
			    (when (or (:expected-message m) (:actual-message m) (:message m)) " ")
			    (when-let [msg (:expected-message m)] (str "           " msg))
			    (when-let [msg (:actual-message m)] (str "           " msg))
			    (when-let [msg (:message m)] (str "           " msg))])))

(defmethod report :error [{:keys [result raw] :as m}]
	   (inc-report-counter :error)
	   (fail *test-name* *test-meta*
		 (str-join "\n"
			   [(when raw (str "           " (raw-str raw)))
			    (when-let [msg (:expected-message m)] (str "  exp-msg: " msg))
			    (when-let [msg (:actual-message m)] (str "  act-msg: " msg))
			    (if (instance? AssertionError result)
			      (.getMessage result)
			      (str "    threw: " (class result) " - " (.getMessage result)))
			    (pruned-stack-trace result)])))

(defmethod report :summary [{:keys [test pass fail error run-time ignored-expectations]}]
	   (summary (str "\nRan " test " tests containing "
			 (+ pass fail error) " assertions in "
			 run-time " msecs\n"
			 (when (> ignored-expectations 0) (str "IGNORED " ignored-expectations " EXPECTATIONS\n"))
			 fail " failures, " error " errors.")))

;; TEST RUNNING

(defn disable-run-on-shutdown [] (reset! run-tests-on-shutdown false))

(defn test-var [v]
  (when-let [t (var-get v)]
    (let [tn (test-name (meta v))
          tm (meta v)]
      (started tn tm)
      (inc-report-counter :test)
      (binding [*test-name* tn
                *test-meta* tm]
        (try
	  (t)
	  (catch Exception e
	    (println "\nunexpected exception in" tn)
	    (.printStackTrace e))))
      (finished tn tm))))

(defn test-vars [vars ignored-expectations]
  (binding [*report-counters* (ref *initial-report-counters*)]
    (let [start (System/nanoTime)]
      (doseq [v vars] (test-var v))
      ;;;      (dorun (pmap test-var vars))
      (assoc @*report-counters*
        :run-time (int (/ (- (System/nanoTime) start) 1000000))
        :ignored-expectations ignored-expectations))))

(defn run-tests-in-vars [vars]
  (let [summary (assoc (test-vars vars 0) :type :summary)]
    (report summary)
    summary))

(defn test-ns [ns]
  (let [expectations (filter #(:expectation (meta %)) (->> ns ns-interns vals (sort-by str)))]
    (if-let [focused (->> expectations (filter #(:focused (meta %))) seq)]
      (test-vars focused (- (count expectations) (count focused)))
      (test-vars expectations 0))))

(defn run-tests [namespaces]
  (let [summary (assoc (apply merge-with + (map test-ns namespaces)) :type :summary)]
    (report summary)
    summary))

(defn run-all-tests
  ([] (run-tests (all-ns)))
  ([re] (run-tests (filter #(re-matches re (name (ns-name %))) (all-ns)))))

(defmulti nan->keyword class :default :default)

(defmethod nan->keyword java.util.Map [m]
	   (let [f (fn [[k v]] [k (if (and (number? v) (Double/isNaN v)) :DoubleNaN v)])]
	     (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defmethod nan->keyword java.util.List [m]
	   (map #(if (and (number? %) (Double/isNaN %)) :DoubleNaN %) m))

(defmethod nan->keyword :default [m]
	   (if (and (number? m) (Double/isNaN m)) :DoubleNaN m))

(defmulti extended-not= (fn [x y] [(class x) (class y)]) :default :default)

(defmethod extended-not= [Double Double] [x y]
	   (if (and (Double/isNaN x) (Double/isNaN y))
	     false
	     (not= x y)))

(defmethod extended-not= :default [x y] (not= x y))

(defn map-intersection [e a]
  (let [in-both (intersection (set (keys e)) (set (keys a)))]
    (select-keys (merge-with vector e a) in-both)))

(defn ->disagreement [prefix [k [v1 v2]]]
  (if (and (map? v1) (map? v2))
    (str-join
     "\n           "
     (remove nil? (map (partial ->disagreement (str (when prefix (str prefix " {")) k)) (map-intersection v1 v2))))
    (when (extended-not= v1 v2)
      (let [prefix-desc (str (when prefix (str prefix " {")) (pr-str k))
            prefix-space (apply str (take (count prefix-desc) (repeat " ")))]
        (str prefix-desc " expected: " (pr-str v1) "\n           " prefix-space "      was: " (pr-str v2))))))

(defn map-diff-message [e a padding]
  (->>
   (map (partial ->disagreement nil) (map-intersection e a))
   (remove nil?)
   (remove empty?)
   seq))

(defn normalize-keys* [a ks m]
  (if (map? m)
    (reduce into [] (map (fn [[k v]] (normalize-keys* a (conj ks k) v)) (seq m)))
    (conj a ks)))

(defn normalize-keys [m] (normalize-keys* [] [] m))

(defn ->missing-message [m msg item]
  (str (str-join " {" item) " with val " (get-in m item) msg))

(defn map-difference [e a]
  (difference (set (normalize-keys e)) (set (normalize-keys a))))

(defn map-missing-message [e a msg]
  (->>
   (map-difference e a)
   (map (partial ->missing-message e msg))
   seq))

(defn map-compare [e a str-e str-a original-a but-string]
  (if (= (nan->keyword e) (nan->keyword a))
    (report {:type :pass})
    (report {:type :fail
             :actual-message
             (when-let [messages (map-missing-message e a " is in expected, but not in actual")]
               (str-join "\n           " messages))
             :expected-message
             (when-let [messages (map-missing-message a e " is in actual, but not in expected")]
               (str-join "\n           " messages))
             :raw [str-e str-a]
             :result ["expected:" e "\n" but-string original-a]
             :message (when-let [messages (map-diff-message e a "")] (str-join "\n           " messages))})))

(defmulti compare-expr (fn [e a str-e str-a]
			 (cond
			  (isa? e Throwable) ::expect-exception
			  (instance? Throwable e) ::expected-exception
			  (instance? Throwable a) ::actual-exception
			  (fn? e) ::fn
			  (::in-flag a) ::in
			  :default [(class e) (class a)])))

(defmethod compare-expr :default [e a str-e str-a]
	   (if (= e a)
	     (report {:type :pass})
	     (report {:type :fail :raw [str-e str-a]
		      :result ["expected:" (pr-str e)
			       "\n                was:" (pr-str a)]})))

(defmethod compare-expr ::fn [e a str-e str-a]
	   (if (e a)
	     (report {:type :pass})
	     (report {:type :fail :raw [str-e str-a]
		      :result [(pr-str a) "is not" str-e]})))

(defmethod compare-expr ::in [e a str-e str-a]
	   (cond
	    (instance? java.util.List (::in a))
	    (if (seq (filter (fn [item] (= (nan->keyword e) (nan->keyword item))) (::in a)))
	      (report {:type :pass})
	      (report {:type :fail :raw [str-e str-a]
		       :result ["value" (pr-str e) "not found in" (::in a)]}))
	    (instance? java.util.Set (::in a))
	    (if ((::in a) e)
	      (report {:type :pass})
	      (report {:type :fail :raw [str-e str-a]
		       :result ["key" (pr-str e) "not found in" (::in a)]}))
	    (instance? java.util.Map (::in a))
	    (map-compare e (select-keys (::in a) (keys e)) str-e str-a (::in a) "                in:")
	    :default (report {:type :fail :raw [str-e str-a]
			      :result ["You supplied:" (pr-str (::in a))]
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
	   (map-compare e a str-e str-a a "               was:"))

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
			:result ["expected:" e "\n                was:" (pr-str a)]}))))

(defmethod compare-expr [java.util.List java.util.List] [e a str-e str-a]
	   (if (= (nan->keyword e) (nan->keyword a))
	     (report {:type :pass})
	     (let [diff-fn (fn [e a] (seq (difference (set e) (set a))))]
	       (report {:type :fail
			:actual-message (when-let [v (diff-fn e a)]
					  (str (str-join ", " v) " are in expected, but not in actual"))
			:expected-message (when-let [v (diff-fn a e)]
					    (str (str-join ", " v) " are in actual, but not in expected"))
			:raw [str-e str-a]
			:result ["expected:" e "\n                was:" (pr-str a)]
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
				  "expected is larger than actual")}))))

(defmacro do-state-based-expect [e a]
  `(let [e# (try ~e (catch Throwable t# t#))
         a# (try ~a (catch Throwable t# t#))]
     (compare-expr e# a# ~(str e) ~(str a))))

(defn fn-string [f-name f-args]
  (str "(" f-name (when f-args " ") (str-join " " f-args) ")"))

(defmulti compare-args (fn [_ e a _ _]
			 (cond
			  (instance? Throwable e) ::expected-exception
			  (instance? Throwable a) ::actual-exception
			  :default ::default)))

(defmethod compare-args ::actual-exception [f e a str-e str-a]
	   (report {:type :error
		    :raw [str-e str-a]
		    :actual-message (str "an exception occurred while evaluating the forms within (during)"
					 "\n           "
					 str-a)
		    :result a}))

(defmethod compare-args ::expected-exception [f e a str-e str-a]
	   "\n           "
	   (report {:type :error
		    :raw [str-e str-a]
		    :expected-message (str "an exception occurred while evaluating the expected args"
					   "\n           "
					   str-e)
		    :result e}))

(defmethod compare-args ::default [f e a _ _]
	   (if (contains? a e)
	     (report {:type :pass})
	     (if (empty? a)
	       (report {:type :fail
			:result ["expected:" (fn-string f e)
				 "\n                but:" f "was never called"]})
	       (report {:type :fail
			:result (apply
				 list
				 "expected:" (fn-string f e)
				 "\n                got:" (fn-string f (first a))
				 (map
				  (comp (partial str "\n                  &: ")
					(partial fn-string f))
				  (rest a)))}))))


(defmacro do-behavior-based-expect [[f & args] [during & forms]]
  `(let [actual-args# (atom #{})
         expected-args# (try ~(if (seq args) (vec args) nil) (catch Throwable t# t#))]
     (binding [~f (fn [& as#] (swap! actual-args# conj as#))]
       (try ~@forms
	    (catch Throwable t#
	      (reset! actual-args# t#))))
     (compare-args '~f expected-args# @actual-args# ~(fn-string f args) ~(fn-string during forms))))

(defmacro doexpect [e a]
  (if
      (and
       (instance? clojure.lang.PersistentList a)
       (= (symbol "during") (first a)))
    `(do-behavior-based-expect ~e ~a)
    `(do-state-based-expect ~e ~a)))

(defmacro expect [e a]
  `(def ~(vary-meta (gensym) assoc :expectation true)
	(fn [] (doexpect ~e ~a))))

(defmacro expect-focused [e a]
  `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
	(fn [] (doexpect ~e ~a))))

(defmacro given [bindings form & args]
  (if args
    `(clojure.template/do-template ~bindings ~form ~@args)
    `(clojure.template/do-template [~'x ~'y] ~(list 'expect 'y (list 'x bindings)) ~@(rest form))))

(defn in [n] {::in n ::in-flag true})

(->
 (Runtime/getRuntime)
 (.addShutdownHook
  (proxy [Thread] []
    (run [] (when @run-tests-on-shutdown (run-all-tests))))))