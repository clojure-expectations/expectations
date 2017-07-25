(ns expectations.clojure.test
  "This namespace provides compatibility with clojure.test and related tooling."
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [expectations :as e]
            [expectations.platform :as p]))

(defn ->test [s x]
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

#?(:clj
    (defmacro expect-test [l e a]
      `(let [l# ~l
             f# ~*file*
             e# (p/try ~e (catch t# t#))
             a# (p/try ~a (catch t# t#))]
         (t/report
          (let [r#
                (p/try (assoc (e/compare-expr e# a# '~e '~a)
                              :expected (->test '~e e#))
                       (catch e2#
                         (assoc (e/compare-expr e2# a# '~e '~a)
                                :actual (->test '~e e2#))))]
            (assoc r# :line l# :file f#
                   :actual (->test '~a a#)))))))

(defn- contains-expect?
  [e]
  (when (and (coll? e) (not (vector? e)))
    (or (= 'expect (first e))
        (some contains-expect? e))))

(defn- translate-expect
  [l form]
  (if (and (coll? form) (not (vector? form)))
    (if (= 'expect (first form))
      (condp = (count form)
             3 (let [[_ e a] form]
                 `(expect-test ~l ~e ~a))
             2 (let [[_ a] form]
                 `(expect-test ~l true (if ~a true false)))
             (map (partial translate-expect l) form))
      (map (partial translate-expect l) form))
    form))

#?(:clj
    (defmacro defexpect [n & body]
      (let [body# (if (and (= 2 (count body))
                           (not (some contains-expect? body)))
                    `((~'expect ~@body))
                    body)
            forms# (map (partial translate-expect (:line (meta &form))) body#)]
        (assert (some contains-expect? body#)
                "defexpect contains no 'expect' forms")
        `(clojure.test/deftest ~n ~@forms#))))

#?(:clj
    (e/disable-run-on-shutdown))
