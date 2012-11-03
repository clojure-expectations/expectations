(ns failure.failure-examples
  (:use expectations)
  (:require success.success-examples-src))

(defn two [] (/ 12 0))
(defn one [] (two))
(defrecord Foo [a b c])
(defmacro a-macro [& args]
  `(println ~@args))

;; errors
(expect 1 (one))

(expect (one) 1)

;; number equality
(expect 1 (identity 2))

;; string equality
(expect "foos" (identity "foo"))

;; map equality
(expect {:foo 2 :bar 3 :dog 3 :car 4} (assoc {} :foo 1 :bar "3" :cat 4))

;; record equality
(expect (->Foo :a :b :c) (->Foo :c :b :a))

;; list equality
(expect [1 2 3 2 4] [3 2 1 3])

;; set equality
(expect #{:foo :bar :dog :car } (conj #{} :foo :bar :cat ))

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

;; k/v pair in record. matches subset
(expect {:a :a} (in (->Foo :c :b :a)))

;; key in set
(expect "foos" (in (conj #{:foo :bar } "cat")))

;; val in list
(expect "foo" (in (conj ["bar"] :foo )))

;; val in nil
(expect "foo" (in nil))

;; expect boolean
(expect empty? (list 1))

;; Double/NaN equality in a set
(expect #{1 9} #{1 Double/NaN})

;; Double/NaN equality with in fn and set
(expect Double/NaN (in #{1}))

;; Double/NaN equality in a list
(expect [1 Double/NaN] [1])

;; Double/NaN equality with in fn and list
(expect Double/NaN (in [1]))

;; Double/NaN equality in a map
(expect {:a Double/NaN :b {:c 9}} {:a Double/NaN :b {:c Double/NaN}})

;; Double/NaN equality with in fn and map
(expect {:a Double/NaN :b {:c 9}} (in {:a Double/NaN :b {:c Double/NaN} :d "other stuff"}))

;; macro expansion
(expect '(clojure.core/println 1 2 (println 100) 3)
        (expanding (a-macro 1 2 (println 101) 3)))

;; multiple expects with form
(given [x y] (expect x (+ y y))
       6 4
       12 12)

(given [x y] (expect 10 (+ x y))
       6 3
       12 -20)

(given [x y] (expect x (in y))
       :c #{:a :b }
       {:a :z} {:a :b :c :d})

(given [x y] (expect x y)
       nil? 1
       fn? 1
       empty? [1])

;; multiple expects on a java instance
(given (java.util.ArrayList.)
       (expect
        .size 1
        .isEmpty false))

;; multiple expects on an instance
(given [1 2 3]
       (expect
        first 0
        last 4))

(given {:a 2 :b 4}
       (expect
        :a 99
        :b 100))

;; nested issues
(expect {nil {nil nil} :z 1 :a 9 :b {:c Double/NaN :d 1 :e 2 :f {:g 10 :i 22}}}
        {:x 1 :a Double/NaN :b {:c Double/NaN :d 2 :e 4 :f {:g 11 :h 12}}})

(expect "the cat jumped over the moon" "the cat jumped under the moon")

(expect (interaction (one))
        (do ))

(expect (interaction (one) :never) (one))

(expect (interaction (one) :twice) (do))

(expect (interaction (one) :twice) (one))

(expect (interaction (one) :twice) (do (one) (one) (one)))

(expect :original
        (with-redefs [success.success-examples-src/an-atom (atom :original)]
          (redef-state [success.success-examples-src]
                       (reset! success.success-examples-src/an-atom :something-else)
                       @success.success-examples-src/an-atom)))

(expect :atom
        (with-redefs [success.success-examples-src/an-atom (atom :original)]
          (do
            (redef-state [success.success-examples-src]
                         (reset! success.success-examples-src/an-atom :atom))
            @success.success-examples-src/an-atom)))

(expect-let [x 2]
            4 x)

(expect (interaction (one "hello" {:a :b :c {:dd :ee :ff :gg}}))
        (do
          (one "hello")
          (one "hello" "world" "here")
          (one "hello" {:a 1 2 3})))


(expect (interaction (one "hello"))
                (throw (RuntimeException. "do you see me?")))

(expect-focused (interaction (one
                              (do (throw (RuntimeException. "do you see me?")))))
                (one "hello"))
