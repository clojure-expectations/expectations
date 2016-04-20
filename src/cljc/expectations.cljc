(ns expectations
  (:refer-clojure :exclude [format ns-name])
  (:require [clojure.data]
            [clojure.set :refer [difference]]
            [clojure.string]
            [expectations.platform :as p :refer [format ns-name]])
  #?(:clj
     (:import (clojure.lang Agent Atom Ref)
              (java.io FileNotFoundException)
              (java.util.regex Pattern)
              (org.joda.time DateTimeUtils))))

(defn no-op [& _])

(defn in [n] {::in n ::in-flag true})

;;; GLOBALS
(def run-tests-on-shutdown (atom true))
(def warn-on-iref-updates-boolean (atom false))

(def ^{:dynamic true} *test-name* nil)
(def ^{:dynamic true} *test-meta* {})
(def ^{:dynamic true} *test-var* nil)
(def ^{:dynamic true} *prune-stacktrace* true)

(def ^{:dynamic true} *report-counters* nil)                ; bound to a ref of a map in test-ns

(def initial-report-counters                                ; used to initialize *report-counters*
  {:test 0, :pass 0, :fail 0, :error 0 :run-time 0})

(def ^{:dynamic true} reminder nil)

;;; UTILITIES FOR REPORTING FUNCTIONS
(defn show-raw-choice []
  (if-let [choice (p/getenv "EXPECTATIONS_SHOW_RAW")]
    (= "TRUE" (clojure.string/upper-case choice))
    true))

(defn colorize-choice []
  (clojure.string/upper-case (or (p/getenv "EXPECTATIONS_COLORIZE")
                                 (str (not (p/on-windows?))))))

(def ansi-colors {:reset   "[0m"
                  :red     "[31m"
                  :blue    "[34m"
                  :yellow  "[33m"
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

(defn- inc-counter! [counters name]
  (assoc counters name (inc (or (counters name) 0))))

(defn inc-report-counter [name]
  (when *report-counters*
    (swap! *report-counters* inc-counter! name)))

;;; TEST RESULT REPORTING
(defn test-name [{:keys [line ns]}]
  (str ns ":" line))

(defn test-file [{:keys [file line]}]
  (colorize-filename (str (last (re-seq #"[0-9A-Za-z_\.]+" file)) ":" line)))

(defn raw-str [[e a]]
  (with-out-str (p/pprint `(~'expect ~e ~a))))

(defn pp-str [e]
  (clojure.string/trim (with-out-str (p/pprint e))))

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

(defn- stackline->str [{:keys [className methodName fileName lineNumber]}]
  (if (= methodName "invoke")
    (str "           on (" fileName ":" lineNumber ")")
    (str "           " className "$" methodName " (" fileName ":" lineNumber ")")))

(defn pruned-stack-trace [t]
  #?(:clj (string-join "\n"
                       (distinct (map stackline->str
                                      (remove ignored-fns (map bean (.getStackTrace t))))))
     :cljs (.-stack t)))                                       ;TODO: proper impl for cljs

(defn ->failure-message [{:keys [raw ref-data result expected-message actual-message message list show-raw]}]
  (string-join "\n"
    [(when reminder
       (colorize-warn (str "     ***** "
                        (clojure.string/upper-case reminder)
                        " *****")))
     (when raw (when (or show-raw (show-raw-choice)) (colorize-raw (raw-str raw))))
     (when-let [[n1 v1 & _] ref-data]
       (format "             locals %s: %s" n1 (pr-str v1)))
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
  (alter-meta! *test-var* assoc ::run true :status [:success "" (:line *test-meta*)])
  (inc-report-counter :pass))

(defmethod report :fail [m]
  (inc-report-counter :fail)
  (let [current-test *test-var*
        message (->failure-message m)]
    (alter-meta! current-test assoc ::run true :status [:fail message (:line *test-meta*)])
    (fail *test-name* *test-meta* message)))

(defmethod report :error [{:keys [result raw] :as m}]
  (inc-report-counter :error)
  (let [result (first result)
        current-test *test-var*
        message (string-join "\n"
                  [(when reminder (colorize-warn (str "     ***** " (clojure.string/upper-case reminder) " *****")))
                   (when raw
                     (when (show-raw-choice) (colorize-raw (raw-str raw))))
                   (when-let [msg (:expected-message m)] (str "  exp-msg: " msg))
                   (when-let [msg (:actual-message m)] (str "  act-msg: " msg))
                   (str "    threw: " (type result) " - " (p/get-message result))
                   (pruned-stack-trace result)])]
    (alter-meta! current-test
      assoc ::run true :status [:error message (:line *test-meta*)])
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

(defn add-watch-every-iref-for-updates [iref-vars]
  (doseq [var iref-vars]
    (add-watch @var ::expectations-watching-state-modifications
      (fn [_ reference old-state new-state]
        (println (colorize-warn
                   (clojure.string/join " "
                     ["WARNING:"
                      (or *test-name* "test name unset")
                      "modified" var
                      "from" (pr-str old-state)
                      "to" (pr-str new-state)])))
        (when-not *test-name*
          (p/print-stack-trace (-> "stacktrace for var modification"
                                   #?(:clj RuntimeException.
                                      :cljs js/Error.))))))))

(defn remove-watch-every-iref-for-updates [iref-vars]
  (doseq [var iref-vars]
    (remove-watch @var ::expectations-watching-state-modifications)))

(defn test-var [v]
  (when-let [t @v]
    (let [tn (test-name (meta v))
          tm (meta v)]
      (started tn tm)
      (inc-report-counter :test)
      (binding [*test-name* tn
                *test-meta* tm
                *test-var* v]
        (try
          (t)
          (catch #?(:clj Throwable
                    :cljs js/Error) e
            (println "\nunexpected error in" tn)
            (p/print-stack-trace e))))
      (finished tn tm))))

(defn execute-vars [vars]
  (doseq [var vars]
    (when (p/bound? var)
      (when-let [vv @var]
        (vv)))))

(defn create-context [in-context-vars work]
  (case (count in-context-vars)
    0 (work)
    1 (@(first in-context-vars) work)
    (do
      (println "expectations only supports 0 or 1 :in-context fns. Ignoring:" in-context-vars)
      (work))))

(defn test-vars [vars-by-kind ignored-expectations]
  #?(:clj (remove-ns 'expectations-options))
  #?(:clj (try
            (require 'expectations-options :reload)
            (catch FileNotFoundException e)))
  (execute-vars (:before-run vars-by-kind))
  (when @warn-on-iref-updates-boolean
    (add-watch-every-iref-for-updates (:iref vars-by-kind)))
  (binding [*report-counters* (atom initial-report-counters)]
    (let [ns->vars (group-by (comp :ns meta) (sort-by (comp :line meta) (:expectation vars-by-kind)))
          start (p/nano-time)
          in-context-vars (vec (:in-context vars-by-kind))]
      (doseq [[a-ns the-vars] ns->vars]
        (doseq [v the-vars]
          (create-context in-context-vars ^{:the-var v} #(test-var v))
          (expectation-finished v))
        (ns-finished (ns-name a-ns)))
      (let [result (assoc @*report-counters*
                     :run-time (int (/ (- (p/nano-time) start) 1000000))
                     :ignored-expectations ignored-expectations)]
        (when @warn-on-iref-updates-boolean
          (remove-watch-every-iref-for-updates (:iref vars-by-kind)))
        (execute-vars (:after-run vars-by-kind))
        result))))

(defn run-tests-in-vars [vars-by-kind]
  (doto (assoc (test-vars vars-by-kind 0) :type :summary)
    (report)))

#?(:clj
   (defn ->vars [ns]
     (->> ns
          ns-name
          ns-interns
          vals
          (sort-by str))))

(defn var-kind [v]
  (let [m (meta v)]
    (cond (and (:focused m)
               (:expectation m)) :focused
          (:expectation m) :expectation
          (:expectations-options m) (:expectations-options m)
          (p/iref-types (type @v)) :iref)))

(defn by-kind [vars]
  (->> vars
    (filter (comp not ::run meta))
    (filter (comp not nil? var-kind))
    (group-by var-kind)))

#?(:clj
   (defn run-tests [namespaces]
     (let [vars-by-kind (by-kind (mapcat ->vars namespaces))
           expectations (:expectation vars-by-kind)]
       (if-let [focused (:focused vars-by-kind)]
         (doto (assoc (test-vars (assoc vars-by-kind :expectation focused) (- (count expectations) (count focused)))
                      :type :summary)
           (report))
         (doto (assoc (test-vars vars-by-kind 0)
                      :type :summary)
           (report))))))

#?(:clj
   (defn run-all-tests
     ([] (run-tests (all-ns)))
     ([re] (run-tests (filter #(re-matches re (name (ns-name %))) (all-ns))))))

(defprotocol CustomPred
  (expect-fn [e a])
  (expected-message [e a str-e str-a])
  (actual-message [e a str-e str-a])
  (message [e a str-e str-a]))

(defmulti compare-expr (fn [e a _ _]
                         (cond
                           (satisfies? CustomPred e) ::custom-pred
                           (and (map? a) (not (sorted? a)) (contains? a ::from-each-flag)) ::from-each
                           (and (map? a) (not (sorted? a)) (contains? a ::in-flag)) ::in
                           (and (map? e) (not (sorted? e)) (contains? e ::more)) ::more
                           (= e a) ::equals
                           (and (string? e) (string? a)) ::strings
                           (and (map? e) (map? a)) ::maps
                           (and (set? e) (set? a)) ::sets
                           (and (sequential? e) (sequential? a)) ::sequentials
                           (and (instance? #?(:clj Pattern :cljs js/RegExp) e)
                                (instance? #?(:clj Pattern :cljs js/RegExp) a)) ::regexps
                           (instance? #?(:clj Pattern :cljs js/RegExp) e) ::re-seq
                           (isa? e #?(:clj Throwable :cljs js/Error)) ::expect-exception
                           (instance? #?(:clj Throwable :cljs js/Error) e) ::expected-exception
                           (instance? #?(:clj Throwable :cljs js/Error) a) ::actual-exception
                           (and (instance? #?(:clj Class :cljs js/Function) e)
                                (instance? #?(:clj Class :cljs js/Function) a)) ::types
                           (and (instance? #?(:clj Class :cljs js/Function) e)
                                (not (and (fn? e) (e a)))) ::expect-instance
                           (fn? e) ::fn
                           :default ::default)))

(defmethod compare-expr ::equals [e a str-e str-a]
  {:type :pass})

(defmethod compare-expr ::default [e a str-e str-a]
  {:type   :fail :raw [str-e str-a]
   :result ["expected:" (pr-str e)
            "\n                was:" (pr-str a)]})

(defmethod compare-expr ::custom-pred [e a str-e str-a]
  (if (expect-fn e a)
    {:type :pass}
    {:type             :fail
     :raw              [str-e str-a]
     :expected-message (expected-message e a str-e str-a)
     :actual-message   (actual-message e a str-e str-a)
     :message          (message e a str-e str-a)}))

(defmethod compare-expr ::fn [e a str-e str-a]
  (try
    (if (e a)
      {:type :pass}
      {:type :fail :raw [str-e str-a] :result [(pr-str a) "is not" str-e]})
    (catch #?(:clj Exception :cljs js/Error) ex
      {:type             :fail :raw [str-e str-a]
       :expected-message (str "also attempted: (" str-e " " str-a ")")
       :actual-message   (str "       and got: " (p/get-message ex))
       :result           ["expected:" str-e
                          "\n                was:" (pr-str a)]})))

(defn find-failures [the-seq]
  (seq (doall (remove (comp #{:pass} :type) the-seq))))

(defn find-successes [the-seq]
  (first (filter (comp #{:pass} :type) the-seq)))

(defmethod compare-expr ::from-each [e {a ::from-each str-i-a ::from-each-body} str-e str-a]
  (if-let [failures (find-failures (for [{ts ::the-result rd ::ref-data} a]
                                     (assoc (compare-expr e ts str-e str-i-a)
                                       :ref-data rd)))]
    {:type    :fail
     :raw     [str-e str-a]
     :message (format "the list: %s" (pr-str (map (fn [x] (if-let [y (::in x)] y x))
                                               (map ::the-result a))))
     :list    (mapv #(assoc % :show-raw true) failures)}
    {:type :pass}))

(defmethod compare-expr ::more [{es ::more} a str-e str-a]
  (if-let [failures (find-failures (for [{:keys [e str-e a-fn gen-str-a]} es]
                                     (compare-expr
                                       e
                                       (try (a-fn a) (catch #?(:clj Throwable :cljs js/Error) t t))
                                       str-e (gen-str-a str-a))))]
    {:type    :fail
     :raw     [str-e str-a]
     :message (format "actual val: %s" (pr-str a))
     :list    (mapv #(assoc % :show-raw true) failures)}
    {:type :pass}))

(defmethod compare-expr ::in [e a str-e str-a]
  (cond
    (or (sequential? (::in a)) (set? (::in a)))
    (if (find-successes (for [a (::in a)]
                          (compare-expr e a str-e str-a)))
      {:type :pass}
      {:type   :fail
       :raw    [str-e str-a]
       :list   (map #(assoc % :show-raw true) (find-failures
                                                (for [a (::in a)]
                                                  (compare-expr e a str-e a))))
       :result [(if (::more e) str-e (format "val %s" (pr-str e))) "not found in" (::in a)]})
    (and (map? (::in a)) (::more e))
    {:type    :fail :raw [str-e str-a]
     :message "Using both 'in with a map and 'more is not supported."}
    (map? (::in a))
    (let [a (::in a)]
      (if (= e (select-keys a (keys e)))
        {:type :pass}
        {:type             :fail
         :expected-message (format "in expected, not actual: %s" (first (clojure.data/diff e a)))
         :actual-message   (format "in actual, not expected: %s" (first (clojure.data/diff a e)))
         :raw              [str-e str-a]
         :result           ["expected:" (pr-str e) "in" (pr-str a)]}))
    :default {:type    :fail :raw [str-e str-a]
              :result  ["You supplied:" (pr-str (::in a))]
              :message "You must supply a list, set, or map when using (in)"}))

(defmethod compare-expr ::expect-instance [e a str-e str-a]
  (if (instance? e a)
    {:type :pass}
    {:type             :fail :raw [str-e str-a]
     :expected-message (str "expected: " a " to be an instance of " e)
     :actual-message   (str "     was: " a " is an instance of " (type a))}))

(defmethod compare-expr ::types [e a str-e str-a]
  (if (isa? a e)
    {:type :pass}
    {:type             :fail :raw [str-e str-a]
     :expected-message (str "expected: " a " to be a " e)}))

(defmethod compare-expr ::actual-exception [e a str-e str-a]
  {:type           :error
   :raw            [str-e str-a]
   :actual-message (str "exception in actual: " str-a)
   :result         [a]})

(defmethod compare-expr ::expected-exception [e a str-e str-a]
  {:type             :error
   :raw              [str-e str-a]
   :expected-message (str "exception in expected: " str-e)
   :result           [e]})

(defmethod compare-expr ::regexps [e a str-e str-a]
  (compare-expr (str e) (str a) str-e str-a))

(defmethod compare-expr ::re-seq [e a str-e str-a]
  (if (re-seq e a)
    {:type :pass}
    {:type   :fail,
     :raw    [str-e str-a]
     :result ["regex" (pr-str e) "not found in" (pr-str a)]}))

(defmethod compare-expr ::strings [e a str-e str-a]
  (let [matches (->> (map vector e a) (take-while (partial apply =)) (map first) (apply str))
        e-diverges (clojure.string/replace e matches "")
        a-diverges (clojure.string/replace a matches "")]
    {:type    :fail :raw [str-e str-a]
     :result  ["expected:" (pr-str e)
               "\n                was:" (pr-str a)]
     :message (str
                "matches: " (pr-str matches)
                "\n           diverges: " (pr-str e-diverges)
                "\n                  &: " (pr-str a-diverges))}))

(defmethod compare-expr ::expect-exception [e a str-e str-a]
  (if (instance? e a)
    {:type :pass}
    {:type   :fail :raw [str-e str-a]
     :result [str-a "did not throw" str-e]}))

(defmethod compare-expr ::maps [e a str-e str-a]
  (let [[in-e in-a] (clojure.data/diff e a)]
    (if (and (nil? in-e) (nil? in-a))
      {:type :pass}
      {:type             :fail
       :expected-message (some->> in-e (format "in expected, not actual: %s"))
       :actual-message   (some->> in-a (format "in actual, not expected: %s"))
       :raw              [str-e str-a]
       :result           ["expected:" (pr-str e) "\n                was:" (pr-str a)]})))

(defmethod compare-expr ::sets [e a str-e str-a]
  {:type             :fail
   :actual-message   (format "in actual, not expected: %s" (first (clojure.data/diff a e)))
   :expected-message (format "in expected, not actual: %s" (first (clojure.data/diff e a)))
   :raw              [str-e str-a]
   :result           ["expected:" e "\n                was:" (pr-str a)]})

(defmethod compare-expr ::sequentials [e a str-e str-a]
  (let [diff-fn (fn [e a] (seq (difference (set e) (set a))))]
    {:type             :fail
     :actual-message   (format "in actual, not expected: %s" (first (clojure.data/diff a e)))
     :expected-message (format "in expected, not actual: %s" (first (clojure.data/diff e a)))
     :raw              [str-e str-a]
     :result           ["expected:" e "\n                was:" (pr-str a)]
     :message          (cond
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
                         "expected is larger than actual")}))

#?(:clj
   (defmacro doexpect [e a]
     `(let [e# (try ~e (catch ~(p/err-type) t# t#))
            a# (try ~a (catch ~(p/err-type) t# t#))]
        (report
         (try (compare-expr e# a# '~e '~a)
              (catch ~(p/err-type) e2#
                (compare-expr e2# a# '~e '~a)))))))

#?(:clj
   (defn- hashname [[s & _ :as form]]
     (symbol (str (name s) (hash (str form))))))

#?(:clj
   (defmacro expect
     ([a] `(expect true (if ~a true false)))
     ([e a]
      `(def ~(vary-meta (hashname &form) assoc :expectation true)
         (fn [] (doexpect ~e ~a))))))

#?(:clj
   (defmacro expect-focused
     ([a] `(expect-focused true (if ~a true false)))
     ([e a]
      `(def ~(vary-meta (hashname &form) assoc :expectation true :focused true)
         (fn [] (doexpect ~e ~a))))))

#?(:clj
   (defmacro expanding [n]
     (p/expanding n)))

#?(:clj
   (when-not (::hook-set (meta run-tests-on-shutdown))
     (-> (Runtime/getRuntime)
         (.addShutdownHook
          (proxy [Thread] []
            (run [] (when @run-tests-on-shutdown (run-all-tests))))))
     (alter-meta! run-tests-on-shutdown assoc ::hook-set true)))

#?(:clj
   (defn- var->symbol [v]
     (symbol (str (.ns v) "/" (.sym v)))))

(defmulti localize type)
#?(:cljs (defmethod localize cljs.core/Atom [a] (atom @a)))
#?(:clj (defmethod localize Atom [a] (atom @a)))
#?(:clj (defmethod localize Agent [a] (agent @a)))
#?(:clj (defmethod localize Ref [a] (ref @a)))
(defmethod localize :default [v] v)

#?(:clj
   (defn- binding-&-localized-val [var]
     (when (p/bound? var)
       (when-let [vv @var]
         (when (p/iref-types (type vv))
           [(var->symbol var) (list 'localize `(deref ~var))])))))

#?(:clj
   (defn- default-local-vals [namespaces]
     (->>
      namespaces
      (mapcat (comp vals ns-interns))
      (mapcat binding-&-localized-val)
      (remove nil?)
      vec)))

#?(:clj
   (defmacro redef-state [namespaces & forms]
     `(with-redefs ~(default-local-vals namespaces) ~@forms)))

#?(:clj
   (defmacro freeze-time [time & forms]                        ;TODO impl for cljs
     `(try
        (DateTimeUtils/setCurrentMillisFixed (.getMillis ~time))
        ~@forms
        (finally
          (DateTimeUtils/setCurrentMillisSystem)))))

#?(:clj
   (defmacro ^{:private true} assert-args [fnname & pairs]
     `(do (when-not ~(first pairs)
            (throw (IllegalArgumentException.
                    ~(str fnname " requires " (second pairs)))))
          ~(let [more (nnext pairs)]
             (when more
               (list* `assert-args fnname more))))))

#?(:clj
   (defmacro context [[sym-kw val & contexts :as args] & forms]
     (assert-args context
                  (vector? args) "a vector for its contexts"
                  (even? (count args)) "an even number of forms in the contexts vector")
     (if (empty? contexts)
       `(~(symbol (name sym-kw)) ~val
         ~@forms)
       `(~(symbol (name sym-kw)) ~val
         (context ~(vec contexts)
                  ~@forms)))))

#?(:clj
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
       `(hash-map ::from-each (doall (for ~seq-exprs
                                       {::the-result (try ~body-expr
                                                          (catch ~(p/err-type) t# t#))
                                        ::ref-data   ~(vec (interleave vs (map symbol vs)))}))
                  ::from-each-body '~body-expr
                  ::from-each-flag true))))

#?(:clj
   (defmacro more [& expects]
     `{::more [~@(map (fn [e] {:e         e
                               :str-e     `'~e
                               :gen-str-a `(fn [x#] x#)
                               :a-fn      `(fn [x#] x#)})
                      expects)]}))

#?(:clj
   (defmacro more-> [& expect-pairs]
     (assert-args more->
                  (even? (count expect-pairs)) "an even number of forms.")
     `{::more [~@(map (fn [[e a-form]]
                        {:e         e
                         :str-e     `'~e
                         :gen-str-a `(fn [x#] (->> (expanding (-> x# ~a-form))
                                                   (replace {'x# x#})))
                         :a-fn      `(fn [x#] (-> x# ~a-form))})
                      (partition 2 expect-pairs))]}))

#?(:clj
   (defmacro more-of [let-sexp & expect-pairs]
     (assert-args more-of
                  (even? (count expect-pairs)) "an even number of expect-pairs")
     `{::more [~@(map (fn [[e a-form]]
                        {:e         e
                         :str-e     `'~e
                         :gen-str-a `(fn [x#] (list '~'let ['~let-sexp x#]
                                                    '~a-form))
                         :a-fn      `(fn [~let-sexp] ~a-form)})
                      (partition 2 expect-pairs))]}))



#?(:clj
   (defmacro side-effects [fn-vec & forms]
     (assert-args side-effects
                  (vector? fn-vec) "a vector for its fn-vec")
     (let [side-effects-sym (gensym "conf-fn")]
       `(let [~side-effects-sym (atom [])]
          (with-redefs ~(vec (interleave fn-vec (repeat `(fn [& args#] (swap! ~side-effects-sym conj args#)))))
            ~@forms)
          @~side-effects-sym))))
