(ns success.nested.success-examples
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
  #+clj
  (:require [expectations :refer :all]))

;; number equality
(expect 1 (do 1))

;; string equality
(expect "foo" (identity "foo"))

; map equality
(expect {:foo 1 :bar 2 :car 4} (assoc {} :foo 1 :bar 2 :car 4))

;; is the regex in the string
(expect #"foo" (str "boo" "foo" "ar"))

;; does the form throw an expeted exception
(expect ArithmeticException (/ 12 0))

;; verify the type of the result
(expect String "foo")

;; k/v pair in map. matches subset
(expect {:foo 1} (in {:foo 1 :cat 4}))

;; key in set
(expect :foo (in (conj #{:foo :bar} :cat)))

;; val in list
(expect :foo (in (conj [:bar] :foo)))

;; expect boolean
(expect empty? (list))
