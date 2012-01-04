(ns success.success-examples
  (:use expectations))

(defrecord Foo [a b c])

;; number equality
(expect 1 (do 1))

;; string equality
(expect "foo" (identity "foo"))

; map equality
(expect {:foo 1 :bar 2 :car 4} (assoc {} :foo 1 :bar 2 :car 4))

;; record equality
(expect (->Foo :a :b :c) (->Foo :a :b :c))

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

;; allow Double/NaN equality in a map
(expect {:a Double/NaN :b {:c Double/NaN}} {:a Double/NaN :b {:c Double/NaN}})

;; allow Double/NaN equality with in fn and map
(expect {:a Double/NaN :b {:c Double/NaN}} (in {:a Double/NaN :b {:c Double/NaN} :d "other stuff"}))

;; allow Double/NaN equality in a set
(expect #{1 Double/NaN} #{1 Double/NaN})

;; allow Double/NaN equality with in fn and set
(expect Double/NaN (in #{1 Double/NaN}))

;; allow Double/NaN equality in a list
(expect [1 Double/NaN] [1 Double/NaN])

;; allow Double/NaN equality with in fn and list
(expect Double/NaN (in [1 Double/NaN]))

;; easy java object return value testing
(given (java.util.ArrayList.)
       (expect
	.size 0
	.isEmpty true))

;; multiple expects on an instance
(given [1 2 3]
       (expect
	first 1
	last 3))

(given {:a 2 :b 4}
       (expect 
	:a 2
	:b 4))

;; multiple expects with form
(given [x y] (expect 10 (+ x y))
	4 6
	6 4
	12 -2)

(given [x y] (expect x (in y))
	:a #{:a :b}
	{:a :b} {:a :b :c :d})

(given [x y] (expect x y)
	nil? nil
	fn? +
	empty? [])

