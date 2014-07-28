(ns success.success-examples-src
  (:use expectations))

(def an-atom (atom "atom"))
(def a-ref (ref "ref"))
(def an-agent (agent "agent"))

(defn a-fn-to-be-rebound [])

(defrecord ConstantlyTrue []
  CustomPred
  (expect-fn [e a] true)
  (expected-message [e a str-e str-a] (format "expected %s" e))
  (actual-message [e a str-e str-a] (format "actual %s" a))
  (message [e a str-e str-a] (format "%s & %s" str-e str-a)))

;; macro expansion
(defmacro a-macro [& args]
  `(println ~@args))

(defrecord ARecord [data])
