(ns clojure-test.clojure-test
  (:require #?(:clj [expectations :refer :all])
            #?(:clj [expectations.clojure.test :refer :all]
               :cljs [expectations.clojure.test :refer-macros [defexpect]])))

(defexpect each
  42 (from-each [n [41 42 43]] n))

(defexpect inn
  42 (in [41 43]))

(defexpect more-more
  (more number? pos?) -1)

(defexpect fn-fn
  pos? -1)

(defexpect strings
  "Hello World!" "Hello Clojure!")

(defexpect maps
  {:a 1} {:b 2})

(defexpect default
  41 (inc 41))

(defexpect exception
  Throwable (/ 1 1))

(defexpect ex-exception
  (/ 1 0) 1)

(defexpect a-exception
  0 (/ 1 0))

(defexpect grouped
  (expect 42 (from-each [n [41 42 43]] n))
  (expect 42 (in [41 43]))
  (expect (more number? pos?) -1)
  (expect pos? -1)
  (expect "Hello World!" "Hello Clojure!")
  (expect {:a 1} {:b 2})
  (expect 41 (inc 42))
  (expect Throwable (/ 1 1))
  (expect (/ 1 0) 1)
  (expect 0 (/ 1 0)))

;; (expect "x") -- expect truthy
(defexpect truthy
  (expect "x"))

;; check nested expect forms
(defexpect nested
  (let [numbers [41 42 43]]
    (expect 42 (from-each [n numbers] n)))
  (println "\nIn between expectations.")
  (expect 41 (inc 41))
  (println "\nAfter expectations."))
