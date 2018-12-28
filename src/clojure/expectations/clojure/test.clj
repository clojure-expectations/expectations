(ns expectations.clojure.test
  "This namespace provides compatibility with clojure.test and related tooling.
  This namespace should be used standalone, without requiring the 'expectations'
  namespace -- this provides a translation layer from Expectations syntax down
  to clojure.test functionality.

  We do not support ClojureScript in clojure.test mode, sorry."
  (:require [clojure.string :as str]
            [clojure.test :as t]))

;; stub functions for :refer compatibility:
(defn- bad-usage [s]
  `(throw (IllegalArgumentException.
           (str ~s " should only be used inside expect"))))
(defmacro in           [& _] (bad-usage "in"))
(defmacro from-each    [& _] (bad-usage "from-each"))
(defmacro more-of      [& _] (bad-usage "more-of"))
(defmacro more->       [& _] (bad-usage "more->"))
(defmacro more         [& _] (bad-usage "more"))
(defmacro side-effects [& _] (bad-usage "side-effects"))

(defmethod t/assert-expr '=? [msg form]
  ;; (is (=? val-or-pred expr))
  (let [[_ e a] form]
    `(let [e# ~e
           a# ~a
           r# (if (fn? e#) (e# a#) (= e# a#))]
       (if r#
         (t/do-report {:type :pass, :message ~msg,
                       :expected '~form, :actual (if (fn? e#)
                                                   (list '~e a#)
                                                   a#)})
         (t/do-report {:type :fail, :message ~msg,
                       :expected '~form, :actual (if (fn? e#)
                                                   (list '~'not (list '~e a#))
                                                   a#)}))
       r#)))

(defmacro ? [form] `(try ~form (catch Throwable t# t#)))

(defmacro expect
  "Temporary version, just to jump start things.

  Things implemented so far:
  * from-each
  * more-of
  * more->
  * simple predicate test
  * class test
  * exception test
  * regex test
  * simple equality

  Things to implement:
  * more
  * in
  * side-effects
  * redef-state ?
  * freeze-time
  * context / in-context ?"
  ([a] `(t/is ~a))
  ([e a] `(expect ~e ~a true))
  ([e a ex?]
   (cond
    (and (sequential? a) (= 'from-each (first a)))
    (let [[_ bindings & body] a]
      (if (= 1 (count body))
        `(doseq ~bindings
           (expect ~e ~(first body)))
        `(doseq ~bindings
           (expect ~e (do ~@body)))))

    (and (sequential? e) (= 'more-> (first e)))
    (let [es (mapv (fn [[e a->]]
                     (if (and (sequential? a->)
                              (symbol? (first a->))
                              (let [s (name (first a->))]
                                (or (str/ends-with? s "->")
                                    (str/ends-with? s "->>"))))
                       `(expect ~e (~(first a->) (? ~a) ~@(rest a->)) false)
                       `(expect ~e (-> (? ~a) ~a->) false)))
                   (partition 2 (rest e)))]
      `(do ~@es))

    (and (sequential? e) (= 'more-of (first e)))
    (let [es (mapv (fn [[e a]] `(expect ~e ~a))
                   (partition 2 (rest (rest e))))]
      `(let [~(second e) ~a] ~@es))

    (and ex? (symbol? e) (resolve e) (class? (resolve e)))
    (if (isa? (resolve e) Throwable)
      `(t/is (~'thrown? ~e ~a))
      `(t/is (~'instance? ~e ~a)))

    (isa? (type e) java.util.regex.Pattern)
    `(t/is (re-find ~e ~a))

    :else
    `(t/is (~'=? ~e ~a)))))

(comment
  (macroexpand '(expect (more-> 1 :a 2 :b 3 (-> :c :d)) {:a 1 :b 2 :c {:d 4}}))
  (macroexpand '(expect (more-of a 2 a) 4))
  (macroexpand '(expect (more-of {:keys [a b c]} 1 a 2 b 3 c) {:a 1 :b 2 :c 3})))

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
  (if (and (>= 2 (count body))
           (not (some contains-expect? body)))
    `(t/deftest ~n (expect ~@body))
    `(t/deftest ~n ~@body)))

(defmacro expecting
  "The Expectations version of clojure.test/testing."
  [string & body]
  `(t/testing ~string ~@body))

(defn approximately
  "Given a value and an optional delta (default 0.001), return a predicate
  that expects its argument to be within that delta of the given value."
  ([^double v] (approximately v 0.001))
  ([^double v ^double d]
   (fn [x] (<= (- v (Math/abs d)) x (+ v (Math/abs d))))))

(defn functionally
  "Given a pair of functions, return a custom predicate that checks that they
  return the same result when applied to a value. May optionally accept a
  'difference' function that should accept the result of each function and
  return a string explaininhg how they actually differ.
  For explaining strings, you could use expectations/strings-difference.
  (only when I port it across!)

  Right now this produces pretty awful failure messages. FIXME!"
  ([expected-fn actual-fn]
   (functionally expected-fn actual-fn (constantly "not functionally equivalent")))
  ([expected-fn actual-fn difference-fn]
   (fn [x]
     (let [e-val (expected-fn x)
           a-val (actual-fn x)]
       (t/is (= e-val a-val) (difference-fn e-val a-val))))))
