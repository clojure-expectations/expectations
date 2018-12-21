(ns expectations.clojure.test
  "This namespace provides compatibility with clojure.test and related tooling.
  This namespace should be used standalone, without requiring the 'expectations'
  namespace -- this provides a translation layer from Expectations syntax down
  to clojure.test functionality.

  We do not support ClojureScript in clojure.test mode, sorry."
  (:require [clojure.test :as t]))

(defmacro expect
  "Temporary version, just to jump start things."
  ([a] `(clojure.test/is ~a))
  ([e a] `(clojure.test/is (~'= ~e ~a))))

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
    `(clojure.test/deftest ~n (expect ~@body))
    `(clojure.test/deftest ~n ~@body)))

(defmacro expecting
  "The Expectations version of clojure.test/testing."
  [string & body]
  `(clojure.test/testing ~string ~@body))
