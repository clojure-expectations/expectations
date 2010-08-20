(ns expectations-test
  (:use expectations))

;; number equality
(expect (inc 0) (inc 0))

;; string equality
(expect "foo" (identity "foo"))

;; map equality
(expect {:foo 1 :bar 2} (assoc {} :foo 1 :bar 2))

;; is the regex in the string
(expect #"foo" (str "boo" "foo" "ar"))

;; k/v pair in map. matches subset
;(expect {:foos 1} in {:foo 1 :cat 4}) 

;; key in set
;(expect (identity :foo) in (conj #{:foo :bar} :cat)) 


;; does the form throw an expeted exception
;(expect ArithmeticException (/ 12 0))

;; verify the type of the result
;(expect String "foo")

;; failures
(expect (inc 0) (inc 2))

(expect (inc 0) (/ 1 0))
