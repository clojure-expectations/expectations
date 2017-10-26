(ns expectations.clojure.test
  "This namespace provides compatibility with clojure.test and related tooling.
  This namespace can be used standalone, without requiring the 'expectations'
  namespace -- all functionality from that namespace is exposed via this one.

  When this namespace is loaded, the 'run on shutdown' hook is automatically
  disabled -- tests are named functions that get run explicitly instead.

  We do not support ClojureScript in clojure.test mode, sorry."
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [expectations :as e]
            [expectations.platform :as p]))

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
   `(let [e# (p/try ~e (catch t# t#))
          a# (p/try ~a (catch t# t#))]
      (t/do-report
       (let [r#
             (p/try (assoc (e/compare-expr e# a# '~e '~a)
                           :expected (->test '~e e#))
                    (catch e2#
                      (let [ex# (e/compare-expr e2# a# '~e '~a)]
                        (assoc ex# :actual
                               (if (= :error (:type ex#))
                                 e2#
                                 (->test '~e e2#))))))]
         (if (= :error (:type r#))
           (if (and (not (instance? Throwable (:actual r#)))
                    (instance? Throwable a#))
             (assoc r# :actual a#)
             (assoc r# :type :fail :actual (->test '~a a#)))
           (assoc r# :actual (->test '~a a#))))))))

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
