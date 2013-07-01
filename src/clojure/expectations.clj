(ns expectations
  (:use clojure.set)
  (:require clojure.template clojure.string clojure.pprint clojure.data))

(def nothing "no arg given")
(defn no-op [& _])

(defn anything [& _] true)
(defn anything& [& _] true)

(defn a-fn1 [& _])
(defn a-fn2 [& _])
(defn a-fn3 [& _])

(defn in [n] {::in n ::in-flag true})
(defn contains-kvs [& {:as kvs}] {::contains-kvs kvs ::contains-kvs-flag true})

;;; GLOBALS
(def run-tests-on-shutdown (atom true))
(def warn-on-iref-updates-boolean (atom false))
(def lib-namespaces (set (all-ns)))

(def ^{:dynamic true} *test-name* nil)
(def ^{:dynamic true} *test-meta* {})
(def ^{:dynamic true} *test-var* nil)
(def ^{:dynamic true} *prune-stacktrace* true)

(def ^{:dynamic true} *report-counters* nil) ; bound to a ref of a map in test-ns

(def ^{:dynamic true} *initial-report-counters* ; used to initialize *report-counters*
  {:test 0, :pass 0, :fail 0, :error 0 :run-time 0})

(def ^{:dynamic true} reminder nil)

;;; UTILITIES FOR REPORTING FUNCTIONS
(defn getenv [var]
  (System/getenv var))

(defn on-windows? []
  (re-find #"[Ww]in" (System/getProperty "os.name")))

(defn show-raw-choice []
  (if-let [choice (getenv "EXPECTATIONS_SHOW_RAW")]
    (= "TRUE" (clojure.string/upper-case choice))
    true))

(defn colorize-choice []
  (clojure.string/upper-case (or (getenv "EXPECTATIONS_COLORIZE")
                                 (str (not (on-windows?))))))

(def ansi-colors {:reset "[0m"
                  :red     "[31m"
                  :blue    "[34m"
                  :yellow    "[33m"
                  :cyan    "[36m"
                  :green   "[32m"
                  :magenta "[35m"})

(defn ansi [code]
  (str \u001b (get ansi-colors code (:reset ansi-colors))))

(defn color [code & s]
  (str (ansi code) (apply str s) (ansi :reset)))

(defn colorize-filename [s]
  (condp = (colorize-choice)
    "TRUE" (color :magenta s)
    s))

(defn colorize-raw [s]
  (condp = (colorize-choice)
    "TRUE" (color :cyan s)
    s))

(defn colorize-results [pred s]
  (condp = (colorize-choice)
    "TRUE" (if (pred)
             (color :green s)
             (color :red s))
    s))

(defn colorize-warn [s]
  (condp = (colorize-choice)
    "TRUE" (color :yellow s)
    s))

(defn string-join [s coll]
  (clojure.string/join s (remove nil? coll)))

(defn stack->file&line [ex index]
  (let [s (nth (.getStackTrace ex) index)]
    (str (.getFileName s) ":" (.getLineNumber s))))

(defn inc-report-counter [name]
  (when *report-counters*
    (dosync (commute *report-counters* assoc name
                     (inc (or (*report-counters* name) 0))))))

;;; TEST RESULT REPORTING
(defn test-name [{:keys [line ns]}]
  (str ns ":" line))

(defn test-file [{:keys [file line]}]
  (colorize-filename (str (last (re-seq #"[A-Za-z_\.]+" file)) ":" line)))

(defn raw-str [[e a]]
  (with-out-str (clojure.pprint/pprint `(~'expect ~e ~a))))

(defn pp-str [e]
  (clojure.string/trim (with-out-str (clojure.pprint/pprint e))))

(defn ^{:dynamic true} fail [test-name test-meta msg]
  (println (str "\nfailure in (" (test-file test-meta) ") : " (:ns test-meta))) (println msg))

(defn ^{:dynamic true} summary [msg] (println msg))
(defn ^{:dynamic true} started [test-name test-meta])
(defn ^{:dynamic true} finished [test-name test-meta])
(defn ^{:dynamic true} ns-finished [a-ns])
(defn ^{:dynamic true} expectation-finished [a-var])

(defn ^{:dynamic true} ignored-fns [{:keys [className fileName]}]
  (when *prune-stacktrace*
    (or (= fileName "expectations.clj")
        (= fileName "expectations_options.clj")
        (= fileName "NO_SOURCE_FILE")
        (= fileName "interruptible_eval.clj")
        (re-seq #"clojure\.lang" className)
        (re-seq #"clojure\.core" className)
        (re-seq #"clojure\.main" className)
        (re-seq #"java\.lang" className)
        (re-seq #"java\.util\.concurrent\.ThreadPoolExecutor\$Worker" className))))

(defn pruned-stack-trace [t]
  (string-join "\n"
               (distinct (map (fn [{:keys [className methodName fileName lineNumber] :as m}]
                                (if (= methodName "invoke")
                                  (str "           on (" fileName ":" lineNumber ")")
                                  (str "           " className "$" methodName " (" fileName ":" lineNumber ")")))
                              (remove ignored-fns (map bean (.getStackTrace t)))))))

(defn ->failure-message [{:keys [raw ref-data result expected-message actual-message message list show-raw]}]
  (string-join "\n"
               [(when reminder
                  (colorize-warn (str "     ***** "
                                      (clojure.string/upper-case reminder)
                                      " *****")))
                (when raw (when (or show-raw (show-raw-choice)) (colorize-raw (raw-str raw))))
                (when-let [[n1 v1 & _] ref-data]
                  (format "           ref-data %s: %s" n1 (pr-str v1)))
                (when-let [[_ _ & the-rest] ref-data]
                  (when the-rest
                    (->> the-rest
                         (partition 2)
                         (map #(format "                    %s: %s" (first %) (pr-str (second %))))
                         (string-join "\n"))))
                (when result (str "           " (string-join " " result)))
                (when (and result (or expected-message actual-message message)) "")
                (when expected-message (str "           " expected-message))
                (when actual-message (str "           " actual-message))
                (when message (str "           " message))
                (when list
                  (str "\n" (string-join "\n\n"
                                         (map ->failure-message list))))]))

(defmulti report :type)

(defmethod report :pass [m]
  (alter-meta! *test-var* assoc :status [:success "" (:line *test-meta*)])
  (inc-report-counter :pass))

(defmethod report :fail [m]
  (inc-report-counter :fail)
  (let [current-test *test-var*
        message (->failure-message m)]
    (alter-meta! current-test assoc :status [:fail message (:line *test-meta*)])
    (fail *test-name* *test-meta* message)))

(defmethod report :error [{:keys [result raw] :as m}]
  (inc-report-counter :error)
  (let [current-test *test-var*
        message (string-join "\n"
                             [(when reminder (colorize-warn (str "     ***** " (clojure.string/upper-case reminder) " *****")))
                              (when raw
                                (when (show-raw-choice) (colorize-raw (raw-str raw))))
                              (when-let [msg (:expected-message m)] (str "  exp-msg: " msg))
                              (when-let [msg (:actual-message m)] (str "  act-msg: " msg))
                              (str "    threw: " (class result) " - " (.getMessage result))
                              (pruned-stack-trace result)])]
    (alter-meta! current-test
                 assoc :status [:error message (:line *test-meta*)])
    (fail *test-name* *test-meta* message)))

(defmethod report :summary [{:keys [test pass fail error run-time ignored-expectations]}]
  (summary (str "\nRan " test " tests containing "
                (+ pass fail error) " assertions in "
                run-time " msecs\n"
                (when (> ignored-expectations 0) (colorize-warn (str "IGNORED " ignored-expectations " EXPECTATIONS\n")))
                (colorize-results (partial = 0 fail error) (str fail " failures, " error " errors")) ".")))

;; TEST RUNNING

(defn disable-run-on-shutdown [] (reset! run-tests-on-shutdown false))
(defn warn-on-iref-updates [] (reset! warn-on-iref-updates-boolean true))

(defn find-every-iref []
  (->> (all-ns)
       (remove #(re-seq #"(clojure\.|expectations)" (str (.name %))))
       (mapcat (comp vals ns-interns))
       (filter bound?)
       (keep #(when-let [val (var-get %)] [% val]))
       (filter (comp #{clojure.lang.Agent clojure.lang.Atom clojure.lang.Ref} class second))))

(defn add-watch-every-iref-for-updates []
  (doseq [[var iref] (find-every-iref)]
    (add-watch iref ::expectations-watching-state-modifications
               (fn [_ reference old-state new-state]
                 (println (colorize-warn
                           (clojure.string/join " "
                                                ["WARNING:"
                                                 (or *test-name* "test name unset")
                                                 "modified" var
                                                 "from" (pr-str old-state)
                                                 "to" (pr-str new-state)])))
                 (when-not *test-name*
                   (.printStackTrace (RuntimeException. "stacktrace for var modification") System/out))))))

(defn remove-watch-every-iref-for-updates []
  (doseq [[var iref] (find-every-iref)]
    (remove-watch iref ::expectations-watching-state-modifications)))

(defn test-var [v]
  (when-let [t (var-get v)]
    (let [tn (test-name (meta v))
          tm (meta v)]
      (started tn tm)
      (inc-report-counter :test)
      (binding [*test-name* tn
                *test-meta* tm
                *test-var*  v]
        (try
          (t)
          (catch Throwable e
            (println "\nunexpected error in" tn)
            (.printStackTrace e))))
      (finished tn tm))))

(defn find-expectations-vars [option-type]
  (->>
   (all-ns)
   (mapcat (comp vals ns-interns))
   (filter (comp #{option-type} :expectations-options meta))))

(defn execute-vars [vars]
  (doseq [var vars]
    (when (bound? var)
      (when-let [vv (var-get var)]
        (vv)))))

(defn create-context [work]
  (let [vars (find-expectations-vars :in-context)]
    (cond
     (= 0 (count vars)) (work)
     (= 1 (count vars)) ((var-get (first vars)) work)
     :default (do
                (println "expectations only supports 0 or 1 :in-context fns. Ignoring:" vars)
                (work)))))

(defn test-vars [vars ignored-expectations]
  (remove-ns 'expectations-options)
  (try
    (require 'expectations-options :reload)
    (catch java.io.FileNotFoundException e))

  (-> (find-expectations-vars :before-run) (execute-vars))
  (when @warn-on-iref-updates-boolean
    (add-watch-every-iref-for-updates))
  (binding [*report-counters* (ref *initial-report-counters*)]
    (let [ns->vars (group-by (comp :ns meta) vars)
          start (System/nanoTime)]
      (doseq [[a-ns the-vars] ns->vars]
        (doseq [v the-vars]
          (create-context
           #(test-var v))
          (expectation-finished v))
        (ns-finished (ns-name a-ns)))
      (let [result (assoc @*report-counters*
                     :run-time (int (/ (- (System/nanoTime) start) 1000000))
                     :ignored-expectations ignored-expectations)]
        (when @warn-on-iref-updates-boolean
          (remove-watch-every-iref-for-updates))
        (-> (find-expectations-vars :after-run) (execute-vars))
        result))))

(defn run-tests-in-vars [vars]
  (doto (assoc (test-vars vars 0) :type :summary)
    (report)))

(defn ->expectation [ns]
  (->> ns
       ns-interns
       vals
       (sort-by str)
       (filter (comp :expectation meta))))

(defn ->focused-expectations [expectations]
  (->> expectations (filter (comp :focused meta)) seq))

(defn run-tests [namespaces]
  (let [expectations (mapcat ->expectation namespaces)]
    (if-let [focused (->focused-expectations expectations)]
      (doto (assoc (test-vars focused (- (count expectations) (count focused))) :type :summary)
        (report))
      (doto (assoc (test-vars expectations 0) :type :summary)
        (report)))))

(defn run-all-tests
  ([] (run-tests (all-ns)))
  ([re] (run-tests (filter #(re-matches re (name (ns-name %))) (all-ns)))))

(defmulti nan->keyword class :default :default)

(defmethod nan->keyword java.util.Map [m]
  (if (instance? clojure.lang.IRecord m)
    (nan->keyword (into {} (seq m)))
    (let [f (fn [[k v]] [k (if (and (number? v) (Double/isNaN v)) :DoubleNaN v)])]
      (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m))))

(defmethod nan->keyword java.util.List [m]
  (map #(if (and (number? %) (Double/isNaN %)) :DoubleNaN %) m))

(defmethod nan->keyword Double [m]
  (if (Double/isNaN m) :DoubleNaN m))

(defmethod nan->keyword java.util.Set [m]
  (reduce #(conj %1 (if (and (number? %2) (Double/isNaN %2)) :DoubleNaN %2)) #{} m))

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
    (string-join
     "\n           "
     (remove nil? (map
                   (partial ->disagreement
                      (str (when prefix (str prefix " {")) k))
                   (map-intersection v1 v2))))
    (when (extended-not= v1 v2)
      (let [prefix-desc (str (when prefix (str prefix " {")) (pr-str k))
            prefix-space (apply str (take (count prefix-desc) (repeat " ")))]
        (str prefix-desc " expected: "
             (pr-str v1) "\n           " prefix-space "      was: " (pr-str v2))))))

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
  (str (string-join " {" (map pr-str item)) " with val " (pr-str (get-in m item)) msg))

(defn map-difference [e a]
  (difference (set (normalize-keys e)) (set (normalize-keys a))))

(defn map-missing-message [e a msg]
  (->>
   (map-difference e a)
   (map (partial ->missing-message e msg))
   seq))

(defn map-compare [e a str-e str-a original-a but-string]
  (if (= (nan->keyword e) (nan->keyword a))
    {:type :pass}
    {:type :fail
     :actual-message (when-let [messages (map-missing-message e a " is in expected, but not in actual")]
                       (string-join "\n           " messages))
     :expected-message (when-let [messages (map-missing-message a e " is in actual, but not in expected")]
                         (string-join "\n           " messages))
     :raw [str-e str-a]
     :result ["expected:" (pr-str e) "\n" but-string (pr-str original-a)]
     :message (when-let [messages (map-diff-message e a "")] (string-join "\n           " messages))}))

(defmulti compare-expr (fn [e a _ _]
                         (cond
                          (and (not (sorted? a)) (::from-each-flag a)) ::from-each
                          (and (isa? e Throwable) (not= e a)) ::expect-exception
                          (instance? Throwable e) ::expected-exception
                          (instance? Throwable a) ::actual-exception
                          (and (fn? e) (not= e a)) ::fn
                          (and (not (sorted? a)) (::in-flag a)) ::in
                          (and (not (sorted? e)) (::contains-kvs-flag e)) ::contains-kvs
                          :default [(class e) (class a)])))

(defmethod compare-expr :default [e a str-e str-a]
  (if (= e a)
    {:type :pass}
    {:type :fail :raw [str-e str-a]
     :result ["expected:" (pr-str e)
              "\n                was:" (pr-str a)]}))

(defmethod compare-expr ::fn [e a str-e str-a]
  (try
    (if (e a)
      {:type :pass}
      {:type :fail :raw [str-e str-a] :result [(pr-str a) "is not" str-e]})
    (catch clojure.lang.ArityException ex
      {:type :fail :raw [str-e str-a]
       :expected-message (str "also attempted: (" str-e " " str-a ")")
       :actual-message (str   "       and got: " (.getMessage ex))
       :result ["expected:" str-e
                "\n                was:" (pr-str a)]})))

(defmethod compare-expr ::contains-kvs [{e ::contains-kvs} a str-e str-a]
  (compare-expr e (in a) str-e str-a))

(defmethod compare-expr ::from-each [e {a ::from-each str-i-a ::from-each-body} str-e str-a]
  (if-let [failures (seq (remove (comp #{:pass} :type)
                                 (for [{ts ::the-seq rd ::ref-data} a]
                                   (assoc (compare-expr e ts str-e str-i-a)
                                     :ref-data rd))))]
    {:type :fail
     :raw [str-e str-a]
     :message (format "the list: %s" (pr-str (map ::the-seq a)))
     :list (map #(assoc % :show-raw true) failures)}
    {:type :pass}))

(defmethod compare-expr ::in [e a str-e str-a]
  (cond
   (instance? java.util.List (::in a))
   (if (seq (filter (fn [item] (= (nan->keyword e) (nan->keyword item))) (::in a)))
     {:type :pass}
     {:type :fail :raw [str-e str-a]
      :result ["value" (pr-str e) "not found in" (::in a)]})
   (instance? java.util.Set (::in a))
   (if ((nan->keyword (::in a)) (nan->keyword e))
     {:type :pass}
     {:type :fail :raw [str-e str-a]
      :result ["key" (pr-str e) "not found in" (::in a)]})
   (instance? java.util.Map (::in a))
   (map-compare e (select-keys (::in a) (keys e)) str-e str-a (::in a) "                in:")
   :default {:type :fail :raw [str-e str-a]
             :result ["You supplied:" (pr-str (::in a))]
             :message "You must supply a list, set, or map when using (in)"}))

(defmethod compare-expr [Class Object] [e a str-e str-a]
  (if (instance? e a)
    {:type :pass}
    {:type :fail :raw [str-e str-a]
     :expected-message (str "expected: " a " to be an instance of " e)
     :actual-message (str "     was: " a " is an instance of " (class a))}))

(defmethod compare-expr [Class Class] [e a str-e str-a]
  (if (isa? a e)
    {:type :pass}
    {:type :fail :raw [str-e str-a]
     :expected-message (str "expected: " a " to be a " e)}))

(defmethod compare-expr ::actual-exception [e a str-e str-a]
  {:type :error
   :raw [str-e str-a]
   :actual-message (str "exception in actual: " str-a)
   :result a})

(defmethod compare-expr ::expected-exception [e a str-e str-a]
  {:type :error
   :raw [str-e str-a]
   :expected-message (str "exception in expected: " str-e)
   :result e})

(defmethod compare-expr [java.util.regex.Pattern java.util.regex.Pattern] [e a str-e str-a]
  (compare-expr (.pattern e) (.pattern a) str-e str-a))

(defmethod compare-expr [java.util.regex.Pattern Object] [e a str-e str-a]
  (if (re-seq e a)
    {:type :pass}
    {:type :fail,
     :raw [str-e str-a]
     :result ["regex" (pr-str e) "not found in" (pr-str a)]}))

(defmethod compare-expr [String String] [e a str-e str-a]
  (if (= e a)
    {:type :pass}
    (let [matches (->> (map vector e a) (take-while (partial apply =)) (map first) (apply str))
          e-diverges (clojure.string/replace e matches "")
          a-diverges (clojure.string/replace a matches "")]
      {:type :fail :raw [str-e str-a]
       :result ["expected:" (pr-str e)
                "\n                was:" (pr-str a)]
       :message (str
                 "matches: " (pr-str matches)
                 "\n           diverges: " (pr-str e-diverges)
                 "\n                  &: " (pr-str a-diverges))})))

(defmethod compare-expr ::expect-exception [e a str-e str-a]
  (if (instance? e a)
    {:type :pass}
    {:type :fail :raw [str-e str-a]
     :result [str-a "did not throw" str-e]}))

(defmethod compare-expr [java.util.Map java.util.Map] [e a str-e str-a]
  (map-compare e a str-e str-a a "               was:"))

(defmethod compare-expr [java.util.Set java.util.Set] [e a str-e str-a]
  (if (= (nan->keyword e) (nan->keyword a))
    {:type :pass}
    (let [diff-fn (fn [e a] (seq (difference e a)))]
      {:type :fail
       :actual-message (when-let [v (diff-fn e a)]
                         (str (string-join ", " v)
                              " are in expected, but not in actual"))
       :expected-message (when-let [v (diff-fn a e)]
                           (str (string-join ", " v)
                                " are in actual, but not in expected"))
       :raw [str-e str-a]
       :result ["expected:" e "\n                was:" (pr-str a)]})))

(defmethod compare-expr [java.util.List java.util.List] [e a str-e str-a]
  (if (= (nan->keyword e) (nan->keyword a))
    {:type :pass}
    (let [diff-fn (fn [e a] (seq (difference (set e) (set a))))]
      {:type :fail
       :actual-message (when-let [v (diff-fn e a)]
                         (str (string-join ", " (map pr-str v))
                              " are in expected, but not in actual"))
       :expected-message (when-let [v (diff-fn a e)]
                           (str (string-join ", " (map pr-str v))
                                " are in actual, but not in expected"))
       :raw [str-e str-a]
       :result ["expected:" e "\n                was:" (pr-str a)]
       :message (cond
                 (and
                  (= (set e) (set a))
                  (= (count e) (count a))
                  (= (count e) (count (set a))))
                 "lists appear to contain the same items with different ordering"
                 (and (= (set e) (set a)) (< (count e) (count a)))
                 "some duplicate items in actual are not expected"
                 (and (= (set e) (set a)) (> (count e) (count a)))
                 "some duplicate items in expected are not actual"
                 (< (count e) (count a))
                 "actual is larger than expected"
                 (> (count e) (count a))
                 "expected is larger than actual")})))

(defn fn-string [f-name f-args]
  (str "(" f-name (when (seq f-args) " ") (string-join " " (map pr-str f-args)) ")"))

(defn compare-individual-args [idx arg1 arg2 raw-e]
  (let [{:keys [expected-message actual-message result message type]}
        (compare-expr arg1 arg2 raw-e (pr-str arg2))]
    (when-not (= type :pass)
      (str
       "\n           - arg" idx ": " (pr-str arg2)
       (when expected-message (str "\n           " expected-message))
       (when actual-message (str "\n           " actual-message))
       (when message (str "\n           " message))))))

(defn compare-each-arg [result
                        [raw-e-first & raw-e-rest]
                        idx
                        {e-first 0 :as e-args :or {e-first nothing}}
                        {a-first 0 :as a-args :or {a-first nothing}}
                        max-idx]
  (if (> idx max-idx)
    result
    (recur (str result (compare-individual-args idx e-first a-first raw-e-first))
           raw-e-rest
           (inc idx) (vec (rest e-args)) (vec (rest a-args)) max-idx)))

(defn compare-args [raw-e f-name expected-args actual-args]
  (compare-each-arg (str "\n\n           -- got: " (fn-string f-name actual-args))
                    raw-e
                    1
                    (vec expected-args)
                    (vec actual-args)
                    (count expected-args)))

(defn matches? [e-arg a-arg]
  (-> (compare-expr e-arg a-arg nil nil) :type (= :pass)))

(defn matching [e-args a-args]
  (cond
   (and (empty? e-args) (not-empty a-args)) false
   (and (empty? a-args) (not-empty e-args)) false
   :default (let [[e-first & e-rest] e-args
                  [a-first & a-rest] a-args
                  match (matches? e-first a-first)]
              (cond
               (and (nil? e-rest) (nil? a-rest)) match
               (false? match) false
               (= e-first anything&) true
               :default (recur e-rest a-rest)))))

(defn compare-interaction [expected-expr f args interactions
                           {:keys [times-fn times]} raw-times raw-e str-e str-a]
  (let [actual-times (count (filter (partial matching args) interactions))]
    (if (times-fn times actual-times)
      {:type :pass}
      (if (empty? interactions)
        {:type :fail
         :raw [str-e str-a]
         :result ["expected:" expected-expr
                  raw-times
                  "\n                but:" f "was never called"]}
        {:type :fail
         :raw [str-e str-a]
         :result (concat ["expected:" expected-expr raw-times
                          "\n                got:" actual-times "times"]
                         (map (partial compare-args raw-e f args) interactions))}))))

(defn ->number-of-times [times]
  (cond
   (= times :never) 0
   (= times :once) 1
   (= times :twice) 2
   (and (list? times) (number? (first times)) (= :times (last times))) (first times)
   :detault `(throw (RuntimeException.
                     (str '~times
                          " is not a supported number of interactions."
                          " use :never, :once, :twice or"
                          " (x times) where x is a number - e.g. (5 times)")))))

(defn ->times [times]
  (cond
   (nil? times) {:times-fn = :times 1}
   (keyword? times) {:times-fn = :times (->number-of-times times)}
   (and (list? times) (number? (first times)) (= :times (last times))) {:times-fn =
                                                                        :times (first times)}
   (and (list? times) (= 'at-least (first times))) {:times-fn <=
                                                    :times (->number-of-times (last times))}
   (and (list? times) (= 'at-most (first times))) {:times-fn >=
                                                   :times (->number-of-times
                                                           (last times))}
   :default `(throw (RuntimeException.
                     (str '~times
                          " is not a supported number of interaction times."
                          " use :never, :once, :twice, (at-least ..) (at-most ..), or"
                          " (x times) where x is a number - e.g. (5 times).")))))

(defmacro do-fn-interaction-expect [[_ [f & args :as expected-expr] times :as e] a]
  (if-not (resolve f)
    `(report (compare-expr (RuntimeException. (str '~f
                                                   " needs to be a var."
                                                   " Either specify the actual var from the"
                                                   " ns or use expectations/no-op or"
                                                   " expectations/a-fn if you want to pass"
                                                   " a fn around."
                                                   )) nil '~e '~a))
    `(try
       (let [expected-interactions# (atom [])]
         (with-redefs [~f (comp (partial swap! expected-interactions# conj) vector)]
           (let [expected-args# (vector ~@args)]
             (try ~a
                  (report (compare-interaction '~expected-expr
                                               ~(str f)
                                               expected-args#
                                               @expected-interactions#
                                               ~(->times times)
                                               '~times
                                               '~(rest (nth e 1))
                                               '~e '~a))
                  (catch Throwable t#
                    (report (compare-expr nil t# '~e '~a)))))))
       (catch Throwable t#
         (report (compare-expr t# nil '~e '~a))))))

(defn ->number-of-mock-times [times]
  (cond
   (= times :never) 0
   (= times :once) 1
   (= times :twice) 2
   (and (list? times) (number? (first times)) (= :times (last times))) (first times)
   :detault `(throw (RuntimeException.
                     (str '~times
                          " is not a supported number of interactions."
                          " use :never, :once, :twice or"
                          " (x times) where x is a number - e.g. (5 times)")))))

(defn ->mock-times [times]
  (cond
   (nil? times) '(org.mockito.Mockito/times 1)
   (keyword? times) `(org.mockito.Mockito/times ~(->number-of-mock-times times))
   (and (list? times) (number? (first times)) (= :times (last times))) `(org.mockito.Mockito/times ~(first times))
   (and (list? times) (= 'at-least (first times))) `(org.mockito.Mockito/atLeast ~(->number-of-mock-times (last times)))
   (and (list? times) (= 'at-most (first times))) `(org.mockito.Mockito/atMost ~(->number-of-mock-times (last times)))
   :default `(throw (RuntimeException.
                     (str '~times
                          " is not a supported number of interaction times."
                          " use :never, :once, :twice, (at-least ..) (at-most ..), or"
                          " (x times) where x is a number - e.g. (5 times).")))))

(defmulti format-mockito-failure (comp class first vector))
(defmethod format-mockito-failure org.mockito.exceptions.verification.WantedButNotInvoked [e expected-expr times]
  {:type :fail
   :result ["expected:" expected-expr
            times
            "\n" (.getMessage e)]})

(defmethod format-mockito-failure org.mockito.exceptions.verification.junit.ArgumentsAreDifferent [e expected-expr times]
  {:type :fail
   :result ["expected:" expected-expr
            times
            "\n" (.getMessage e)]})

(defmethod format-mockito-failure org.mockito.exceptions.verification.NeverWantedButInvoked [e expected-expr times]
  {:type :fail
   :result ["expected:" expected-expr
            times
            "\n" (.getMessage e)]})

(defmethod format-mockito-failure org.mockito.exceptions.verification.TooLittleActualInvocations [e expected-expr times]
  {:type :fail
   :result ["expected:" expected-expr
            times
            "\n" (.getMessage e)]})

(defmethod format-mockito-failure org.mockito.exceptions.base.MockitoAssertionError [e expected-expr times]
  {:type :fail
   :result ["expected:" expected-expr
            times
            "\n" (.getMessage e)]})

(defmacro do-mock-interaction-expect [[_ [method o & args :as expected-expr] times :as e] a]
  `(try
     ~a
     (try
       (~'verify ~o ~(->mock-times times) (~method ~@args))
       (report {:type :pass})
       (catch org.mockito.exceptions.base.MockitoAssertionError e#
         (report (format-mockito-failure e# '~expected-expr '~times)))
       (catch org.mockito.exceptions.verification.TooLittleActualInvocations e#
         (report (format-mockito-failure e# '~expected-expr '~times)))
       (catch org.mockito.exceptions.verification.WantedButNotInvoked e#
         (report (format-mockito-failure e# '~expected-expr '~times)))
       (catch org.mockito.exceptions.verification.junit.ArgumentsAreDifferent e#
         (report (format-mockito-failure e# '~expected-expr '~times)))
       (catch org.mockito.exceptions.verification.NeverWantedButInvoked e#
         (report (format-mockito-failure e# '~expected-expr '~times)))
       (catch Throwable t#
         (report (compare-expr t# nil '~e '~a))))
     (catch Throwable t#
       (report (compare-expr nil t# '~e '~a)))))

(defmacro do-interaction-expect [[_ [f & args :as expected-expr] times :as e] a]
  (if (= \. (first (name f)))
    `(do-mock-interaction-expect ~e ~a)
    `(do-fn-interaction-expect ~e ~a)))

(defmacro do-value-expect [e a]
  `(let [e# (try ~e (catch Throwable t# t#))
         a# (try ~a (catch Throwable t# t#))]
     (report
      (try (compare-expr e# a# '~e '~a)
           (catch Throwable e2#
             (compare-expr e2# a# '~e '~a))))))

(defmacro doexpect [e a]
  (if (and (list? e) (= 'interaction (first e)))
    `(do-interaction-expect ~e ~a)
    `(do-value-expect ~e ~a)))

(defmacro expect
  ([a] `(expect true (if ~a true false)))
  ([e a]
     `(def ~(vary-meta (gensym) assoc :expectation true)
        (fn [] (doexpect ~e ~a)))))

(defmacro expect-let [bindings e a]
  `(def ~(vary-meta (gensym) assoc :expectation true)
     (fn [] (let ~bindings (doexpect ~e ~a)))))

(defmacro expect-focused
  ([a] `(expect-focused true (if ~a true false)))
  ([e a]
     `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
        (fn [] (doexpect ~e ~a)))))

(defmacro expect-let-focused [bindings e a]
  `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
     (fn [] (let ~bindings (doexpect ~e ~a)))))

(defmacro given [bindings form & args]
  (if args
    `(clojure.template/do-template ~bindings ~form ~@args)
    `(clojure.template/do-template [~'x ~'y] ~(list 'expect 'y (list 'x bindings)) ~@(rest form))))

(defmacro expanding [n] (list 'quote  (macroexpand-1 n)))

(->
 (Runtime/getRuntime)
 (.addShutdownHook
  (proxy [Thread] []
    (run [] (when @run-tests-on-shutdown (run-all-tests))))))

(defn interaction
  ([form])
  ([form times])) ;;; this is never used, but it's nice for auto-completion and documentation

(defn var->symbol [v]
  (symbol (str (.ns v) "/" (.sym v))))

(defmulti localize class)
(defmethod localize clojure.lang.Atom [a] (atom @a))
(defmethod localize clojure.lang.Agent [a] (agent @a))
(defmethod localize clojure.lang.Ref [a] (ref @a))
(defmethod localize :default [v] v)

(defn binding-&-localized-val [var]
  (when (bound? var)
    (when-let [vv (var-get var)]
      (when (#{clojure.lang.Agent clojure.lang.Atom clojure.lang.Ref} (class vv))
        [(var->symbol var) (list 'localize (var->symbol var))]))))

(defn default-local-vals [namespaces]
  (->>
   namespaces
   (mapcat (comp vals ns-interns))
   (mapcat binding-&-localized-val)
   (remove nil?)
   vec))

(defmacro redef-state [namespaces & forms]
  `(with-redefs ~(default-local-vals namespaces) ~@forms))

(defmacro freeze-time [time & forms]
  `(try
     (org.joda.time.DateTimeUtils/setCurrentMillisFixed (.getMillis ~time))
     ~@forms
     (finally
       (org.joda.time.DateTimeUtils/setCurrentMillisSystem))))

(defmacro ^{:private true} assert-args [fnname & pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 ~(str fnname " requires " (second pairs)))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args fnname more)))))

(defmacro context [[sym-kw val & contexts :as args] & forms]
  (assert-args context
               (vector? args) "a vector for its contexts"
               (even? (count args)) "an even number of forms in the contexts vector")
  (if (empty? contexts)
    `(~(symbol (name sym-kw)) ~val
      ~@forms)
    `(~(symbol (name sym-kw)) ~val
      (context ~(vec contexts)
               ~@forms))))

(defmacro from-each [seq-exprs body-expr]
  (let [vs (for [[p1 p2 :as pairs] (partition 2 seq-exprs)
                 :when (and (not= :when p1) (not= :while p1))
                 :let [vars (->> (if (= p1 :let)
                                   p2
                                   pairs)
                                 destructure
                                 (keep-indexed #(when (even? %1) %2))
                                 (map str)
                                 distinct
                                 (remove (partial re-find #"^(map|vec)__\d+$")))]
                 v vars]
             v)]
    `(hash-map ::from-each (for ~seq-exprs
                             {::the-seq ~body-expr
                              ::ref-data ~(vec (interleave vs (map symbol vs)))})
               ::from-each-body '~body-expr
               ::from-each-flag true)))
