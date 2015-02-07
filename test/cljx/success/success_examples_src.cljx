(ns success.success-examples-src
  (:refer-clojure :exclude [format])
  #+cljs
  (:require-macros
    [expectations :refer [expanding
                          expect
                          expect-let
                          from-each
                          more
                          more->
                          more-of
                          redef-state
                          side-effects]])
  (:require #+clj [expectations :refer :all]
            #+cljs [expectations :refer [CustomPred]]
            [expectations.platform :as p :refer [format]]))

(def an-atom (atom "atom"))

(defn a-fn-to-be-rebound [])

(defrecord ConstantlyTrue []
  CustomPred
  (expect-fn [e a] true)
  (expected-message [e a str-e str-a] (format "expected %s" e))
  (actual-message [e a str-e str-a] (format "actual %s" a))
  (message [e a str-e str-a] (format "%s & %s" str-e str-a)))

;; macro expansion
#+clj
(defmacro a-macro [& args]
  `(println ~@args))

(defrecord ARecord [data])

#+clj
(defmacro cljs? []
  (p/cljs?))
