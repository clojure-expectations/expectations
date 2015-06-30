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
(expect "foos" (identity "foode"))

;; map equality
(expect {:foo 1 :bar 3 :dog 3 :car 4} (assoc {} :foo 1 :bar "3" :cat 4))

;; record equality
(expect (->Foo :a :b :c) (->Foo :c :b :a))

;; list equality
(expect [1 2 3 2 4] [3 2 1 3]) ;; expected larger msg
(expect [3 2 1 3] [1 2 3 2 4]) ;; actual larger msg
(expect [1 2 3 5] [5 2 1 3]) ;; wrong ordering msg
(expect [2 2 1 3] [2 1 3]) ;; dupes in expected, not actual msg
(expect [2 1 3] [2 2 1 3]) ;; dupes in actual, not expected msg
(expect [99 1 2] [100 1 3]) ;; show diff results

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
(expect {:a :a :b :b} (in (->Foo :c :b :a)))

;; key in set
(expect "foos" (in (conj #{:foo :bar } "cat")))

;; val in list
(expect "foo" (in (conj ["bar"] :foo )))

;; val in nil
(expect "foo" (in nil))

;; expect boolean
(expect empty? (list 1))

;; macro expansion
(expect '(clojure.core/println 1 2 (println 100) 3)
        (expanding (a-macro 1 2 (println 101) 3)))

;; nested issues
(expect {nil {nil nil} :z 1 :a 9 :b {:c Double/NaN :d 1 :e 2 :f {:g 10 :i 22}}}
        {:x 1 :a Double/NaN :b {:c Double/NaN :d 2 :e 4 :f {:g 11 :h 12}}})

(expect "the cat jumped over the moon" "the cat jumped under the moon")

(expect :original
        (with-redefs [success.success-examples-src/an-atom (atom :original)]
          (redef-state [success.success-examples-src]
                       (reset! success.success-examples-src/an-atom :something-else)
                       @success.success-examples-src/an-atom)))

(expect :original
        (with-redefs [success.success-examples-src/an-private-atom (atom :original)]
          (redef-state [success.success-examples-src]
            (reset! @#'success.success-examples-src/an-private-atom :something-else)
            @@#'success.success-examples-src/an-private-atom)))

(expect :atom
        (with-redefs [success.success-examples-src/an-atom (atom :original)]
          (do
            (redef-state [success.success-examples-src]
                         (reset! success.success-examples-src/an-atom :atom))
            @success.success-examples-src/an-atom)))

(expect :atom
        (with-redefs [success.success-examples-src/an-private-atom (atom :original)]
          (do
            (redef-state [success.success-examples-src]
              (reset! @#'success.success-examples-src/an-private-atom :atom))
            @@#'success.success-examples-src/an-private-atom)))

(expect-let [x 2]
            4 x)

(expect nil)
(expect false)

(expect filter map)

(expect even? (from-each [i [1 2 3]]
                         i))

(expect even? (from-each [i [1 2 3]
                          :let [ii (inc i)
                                iii (inc ii)]
                          :let [iiii (inc iii)
                                iiiii (inc iiii)]]
                         (dec iiiii)))

(expect even? (from-each [[i {:keys [v1] {:strs [v3] :syms [v4] :or {v4 9}} :v2 :as all-of-vs} :as z]
                                   [[1 {:v1 3 :v2 {"v3" 5 'v4 7}}]
                                    [3 {:v1 4 :v2 {"v3" 6}}]]
                                   :let [ii (map inc [i v1 v3])
                                         iii (map inc ii)]
                                   j iii
                                   :let [jj (inc j)
                                         jjj (inc jj)]]
                                  (dec jjj)))

(defrecord ConstantlyFalse []
  CustomPred
  (expect-fn [e a] false)
  (expected-message [e a str-e str-a] (format "expected %s" e))
  (actual-message [e a str-e str-a] (format "actual %s" a))
  (message [e a str-e str-a] (format "%s & %s" str-e str-a)))

(expect (->ConstantlyFalse) [1 2 3 4])

(expect 4
  (from-each [x [[1 2] [1 3]]]
    (in x)))

(expect (more-of x list? x
                 vector? x)
  (in (side-effects [spit]
                    (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f})
                    (spit "/tmp/hello-world" "some data" :append 1))))

(expect (more-of x
                 list? x
                 2 (first x))
  (conj [] 1 2 3))

(expect (more-of [x y z :as full-list]
                 list? full-list
                 2 x
                 3 y
                 4 z)
  (conj [] 1 2 3))

(expect (more list? empty?) [1 2 3])

(expect (more-> 1 (-> identity .size)
                false .isEmpty)
  (java.util.ArrayList.))

(expect (more-> 1 (-> first (+ 1))
                4 last)
  (conj [] 1 2 3))

(expect "need to see exceptions here" (from-each [x [1 2 3]]
                                        (/ x 0)))

(expect AssertionError (from-each [a ["2" 1]] (assert (string? a))))
