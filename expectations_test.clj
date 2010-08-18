(ns expectations-test
  (:use expectations))

(expect {:foo 1} in (assoc {:bar 1} :foo 1)) 

(expect :foo in #{:foo :bar}) 

(expect 2 (inc 1))

(expect "foo" "foo")

(expect #"foo" (str "boo" "foo" "ar"))

(expect ArithmeticException (/ 12 0))

(expect String "foo")