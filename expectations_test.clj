(ns expectations-test
  (:use expectations))

;; number equality
(expect 1 (inc 0))

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

;; expect boolean
(expect (list) (is empty?))

;; multiple expects with form
(expect [x y] x (+ y y)
	4 2
	6 3
	12 6)

(expect [x y] x (in y)
	:a #{:a :b}
	{:a :b} {:a :b :c :d})

(expect [x y] x (is y)
	nil nil?
	+ fn?
	[] empty?)

;; todo
;; - loose match in hashes
;; - in with vectors