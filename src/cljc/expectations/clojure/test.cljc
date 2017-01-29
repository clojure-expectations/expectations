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

(defn- expand-expects
  [forms]
  (let [expanded
        (mapcat (fn [e]
                  (if (and (coll? e)
                           (= 'expect (first e)))
                    (condp = (count e)
                           3 (let [[_ e a] e]
                               [e a])
                           2 (let [[_ a] e]
                               [true `(if ~a true false)])
                           (throw (ex-info "Illegal 'expect' form" {:form e})))
                    [e])) forms)]
    (when-not (even? (count expanded))
      (throw (ex-info "defexpect requires an even number of forms" {})))
    expanded))

(defn- inflate-expects
  [l forms]
  (map (fn [[e a]]
         `(expect-test ~l ~e ~a)) forms))

#?(:clj
    (defmacro defexpect [n & body]
      (let [forms# (->> (expand-expects body)
                        (partition 2)
                        (inflate-expects (:line (meta &form))))]
        `(clojure.test/deftest ~n ~@forms#))))

#?(:clj
    (e/disable-run-on-shutdown))
