(ns expectations-test
  (:use expectations))

;; number equality
(expect 2 (inc 0))

;; string equality
(expect "afoo" (identity "foo"))

; map equality
(expect {:afoo 1 :bar 2 :car 4} (assoc {} :foo 1 :bar 2 :car 4))

;; is the regex in the string
(expect #"afoo" (str "boo" "foo" "ar"))

;; does the form throw an expeted exception
(expect ArithmeticException (/ 12 12))

;; verify the type of the result
(expect String 1)

;; k/v pair in map. matches subset
(expect {:foox 1} (in {:foo 1 :cat 4}))

;; key in set
(expect :fooee (in (conj #{:foo :bar} :cat)))
