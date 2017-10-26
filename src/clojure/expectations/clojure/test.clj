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

(defn clean-up-exception
  "Given an expected exception, return a simpler representation of it that
  clojure.test's reporting can display nicely."
  [ex]
  (into [(.getClass ex) (.getMessage ex)]
        ((juxt :fileName :lineNumber)
         (first (remove e/ignored-fns (map bean (.getStackTrace ex)))))))

(defn ->test-report
  "Given a test report map, a symbolic form, a value (produced from it), and
  the next report key (:actual or :expected), merge in more elements to build
  up a test report that clojure.test can use to report the failure, based
  either on the form or the value, or both."
  [m s x k]
  (let [message (:message m)]
    (cond (fn? x)
          (assoc m k s)
          (and (map? x)
               (or (::e/more x)
                   (::e/in-flag x)
                   (::e/from-each-flag x)))
          (assoc m k s)
          (instance? Throwable x)
          (if (= :actual k)
            (assoc m :message (str (when message
                                     (str message "\n"))
                                   "  thrown: "
                                   (.getMessage x)
                                   " from " (pr-str s))
                   k x)
            (assoc m :message (str "  wanted: "
                                   (.getMessage x)
                                   " from "
                                   (pr-str s)
                                   (when message
                                     (str "\n" message)))
                   k (clean-up-exception x)))
          :else
          (let [ss (pr-str s)
                xs (pr-str x)]
            (if (= ss xs)
              (assoc m k s)
              (if (= :actual k)
                (assoc m :message (str (when message
                                         (str message "\n"))
                                       "produced: " xs " from " ss)
                       k s)
                (assoc m :message (str "  wanted: " xs " from " ss
                                       (when message
                                         (str "\n" message)))
                       k s)))))))

(defn fold-messages-together
  "Given a test report, attempt to fold in additional information from our
  Expectations report so that clojure.test can provide more details."
  [m]
  (reduce (fn [r k]
            (if (k r)
              (assoc r :message
                     (str (when (:message r)
                            (str (:message r) "\n"))
                          (if (= :result k)
                            (str "failure is "
                                 (str/join " " (k r)))
                            (k r))))
              r))
          m
          [:result :expected-message :actual-message]))

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
                      (if (= :error (:type ex#))
                        (assoc ex# :actual t#)
                        (->test-report ex# '~e t# :actual)))))]
                ;; if the comparison led to an error, and we didn't go through
                ;; the exception block above and the actual value is an
                ;; exception, then set the actual to that exception...
         (-> (if (= :error (:type r#))
               (if (and (not (instance? Throwable (:actual r#)))
                        (instance? Throwable a#))
                 (assoc r# :actual a#)
                 ;; ...else back the error down to a failure and use a
                 ;; symbolic representation of the actual value
                 (-> r#
                     (assoc :type :fail)
                     (dissoc :result) ; override Expectations message
                     (->test-report '~a a# :actual)))
               ;; otherwise (pass or fail) so set the symbolic actual value
               (->test-report r# '~a a# :actual))
             ;; add in a symbolic representation of the expected value
             (->test-report '~e e# :expected)
             (fold-messages-together)))))))

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
