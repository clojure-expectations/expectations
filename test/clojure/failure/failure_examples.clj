(ns failure.failure-examples
  (:use expectations))

(defn two [] (/ 12 0))
(defn one [] (two))

;; errors
(expect 1 (one))

(expect (one) 1)

(expect (one))

;; number equality
(expect 1 (identity 2))

;; string equality
(expect "foos" (identity "foo"))

;; map equality
(expect {:foo 2 :bar 3 :dog 3 :car 4} (assoc {} :foo 1 :bar "3" :cat 4))

;; list equality
(expect [1 2 3 2 4] [3 2 1 3])

;; set equality
(expect #{:foo :bar :dog :car} (conj #{} :foo :bar :cat))

;; lazy cons printing
(expect [1 2] (map - [1 2]))

;; is the regex in the string
(expect #"foo" (str "boo" "fo" "ar"))

;; does the form throw an expeted exception
(expect ArithmeticException (/ 12 12))

;; verify the type of the result
(expect String 1)

;; k/v pair in map. matches subset
(expect {:foos 1 :cat 5} (in {:foo 1 :cat 4}))

;; key in set
(expect "foos" (in (conj #{:foo :bar} "cat")))

;; val in list
(expect "foo" (in (conj ["bar"] :foo)))

;; val in nil
(expect "foo" (in nil))

;; expect boolean
(expect (empty? (list 1)))

;; double nan fail
(expect {:a Double/NaN :b {:c 9}} {:a Double/NaN :b {:c Double/NaN}})

;; multiple expects with form
(given [x y] (expect x (+ y y))
	6 4
	12 12)

(given [x y] (expect 10 (+ x y))
	6 3
	12 -20)

(given [x y] (expect x (in y))
	:c #{:a :b}
	{:a :z} {:a :b :c :d})

(given [x y] (expect (x y))
	nil? 1
	fn? 1
	empty? [1])

;; todo
;; - loose match in hashes