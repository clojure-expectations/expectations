(ns expectations.clojure.test
  "This namespace provides compatibility with clojure.test and related tooling.
  This namespace can be used standalone, without requiring the 'expectations'
  namespace -- all functionality from that namespace is exposed via this one.

  When this namespace is loaded, the 'run on shutdown' hook is automatically
  disabled -- tests are named functions that get run explicitly instead.

  We do not support ClojureScript in clojure.test mode, sorry."
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [expectations :as e]))

(e/disable-run-on-shutdown)

(def ^:private inapplicable
  "The following symbols in 'expectations' do not apply and/or do not work
  in clojure.test mode so we will not import them."
  #{'expect 'warn-on-iref-updates 'warn-on-iref-updates-boolean})

;; import applicable symbols from 'expectations' namespace as-is:
(doseq [[n v] (ns-publics 'expectations)
        :when (not (inapplicable n))]
  (intern 'expectations.clojure.test
          (with-meta n (meta v))
          (deref v)))

(defn ->test
  "Given a symbolic form and a value (produced from it), attempt to produce a
  symbol that clojure.test can happily report on, based either on the form or
  the value, or both."
  [s x]
  (cond (fn? x) (symbol (pr-str s))
        (and (map? x)
             (or (::e/more x)
                 (::e/in-flag x)
                 (::e/from-each-flag x)))
        (symbol (pr-str s))
        :else
        (let [ss (pr-str s)
              xs (pr-str x)]
          (if (= ss xs) (symbol ss) (symbol (str xs " from " ss))))))

(defmacro expect
  "Expectations' equivalent to clojure.test's 'is' macro."
  ([a] `(expect true (if ~a true false)))
  ([e a]
   `(let [e# (try ~e (catch Throwable t# t#))
          a# (try ~a (catch Throwable t# t#))]
      (t/do-report
       (let [r# (try
                  (e/compare-expr e# a# '~e '~a)
                  (catch Throwable t#
                    ;; if the comparison fails, attempt to match on the
                    ;; exception thrown...
                    (let [ex# (e/compare-expr t# a# '~e '~a)]
                      ;; ...and set the actual to either the thrown exception
                      ;; if the compare led to an error, else a symbolic
                      ;; representation of the exception for a failure
                      (assoc ex# :actual (if (= :error (:type ex#))
                                           t#
                                           (->test '~e t#))))))]
         (merge r#
                ;; add in a symbolic representation of the expected value
                {:expected (->test '~e e#)}
                ;; if the comparison led to an error, and we didn't go through
                ;; the exception block above and the actual value is an
                ;; exception, then set the actual to that exception...
                (if (= :error (:type r#))
                  (if (and (not (instance? Throwable (:actual r#)))
                           (instance? Throwable a#))
                    {:actual a#}
                    ;; ...else back the error down to a failure and use a
                    ;; symbolic representation of the actual value
                    {:type :fail :actual (->test '~a a#)})
                  ;; otherwise (pass or fail) so set the symbolic actual value
                  {:actual (->test '~a a#)})))))))

(defn- contains-expect?
  "Given a form, return true if it contains any calls to the 'expect' macro."
  [e]
  (when (and (coll? e) (not (vector? e)))
    (or (= 'expect (first e))
        (some contains-expect? e))))

(defmacro defexpect
  "Given a name (a symbol that may include metadata) and a test body,
  produce a standard 'clojure.test' test var (using 'deftest').

  (defexpect name expected actual) is a special case shorthand for
  (defexpect name (expect expected actual)) provided as an easy way to migrate
  legacy Expectation tests to the 'clojure.test' compatibility version."
  [n & body]
  (if (and (= 2 (count body))
           (not (some contains-expect? body)))
    `(clojure.test/deftest ~n (expect ~@body))
    `(clojure.test/deftest ~n ~@body)))

(defmacro expecting
  "The Expectations version of clojure.test/testing."
  [string & body]
  `(binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
     ~@body))
