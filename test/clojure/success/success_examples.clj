(ns success.success-examples
  (:use expectations)
  (:require success.success-examples-src)
  (:import [org.joda.time DateTime]))


(expect true)
(expect "x")
(expect (not false))

;; number equality
(expect 1 (do 1))

;; string equality
(expect "foo" (identity "foo"))

;; map equality
(expect {:foo 1 :bar 2 :car 4} (assoc {} :foo 1 :bar 2 :car 4))

;; record equality
(defrecord Foo [a b c])

(expect (->Foo :a :b :c) (->Foo :a :b :c))

;; is the regex in the string
(expect #"foo" (str "boo" "foo" "ar"))

;; does the form throw an expeted exception
(expect ArithmeticException (/ 12 0))

;; verify the type of the result
(expect String "foo")

;; k/v pair in map. matches subset
(expect {:foo 1} (in {:foo 1 :cat 4}))

;; k/v pair in record. matches subset
(expect {:a :a} (in (->Foo :a :b :c)))

;; key in set
(expect :foo (in (conj #{:foo :bar} :cat)))

;; val in list
(expect :foo (in (conj [:bar] :foo)))

;; expect boolean
(expect empty? (list))

;; allow Double/NaN equality in a map
(expect {:a Double/NaN :b {:c Double/NaN}} {:a Double/NaN :b {:c Double/NaN}})

;; allow Double/NaN equality with in fn and map
(expect {:a Double/NaN :b {:c Double/NaN}}
        (in {:a Double/NaN :b {:c Double/NaN} :d "other stuff"}))

;; allow Double/NaN equality in a set
(expect #{1 Double/NaN} #{1 Double/NaN})

;; allow Double/NaN equality with in fn and set
(expect Double/NaN (in #{1 Double/NaN}))

;; allow Double/NaN equality in a list
(expect [1 Double/NaN] [1 Double/NaN])

;; allow Double/NaN equality with in fn and list
(expect Double/NaN (in [1 Double/NaN]))

;; sorted map equality
(expect (sorted-map-by > 1 :a 2 :b) (sorted-map-by > 1 :a 2 :b))

;; macro expansion
(defmacro a-macro [& args]
  `(println ~@args))

(expect '(clojure.core/println 1 2 (println 100) 3)
        (expanding (a-macro 1 2 (println 100) 3)))

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

;; (expect (interaction (spit String "some data" :append true))
;;         (do
;;           (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing
(expect (interaction (spit "/tmp/hello-world" "some data" :append true))
        (do
          (spit "some other stuff" "xy")
          (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing, expect zero interactions
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         :never)
        (spit "some other stuff" "xy"))

;; interaction based testing, expect two interactions
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         :twice)
        (do
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing, expect at least one interaction
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (at-least :once))
        (spit "/tmp/hello-world" "some data" :append true))

;; interaction based testing, expect at least one interaction
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (at-least :once))
        (do
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing, expect at most one interaction
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (at-most :once))
        (spit "/tmp/hello-world" "some data" :append true))

;; interaction based testing, expect at most one interaction
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (at-most :once))
        (do))

;; interaction based testing, expect exactly 2 interactions
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (2 :times))
        (do
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing, expect exactly 3 interactions
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (3 :times))
        (do
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing, expect at least 2 interactions
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (at-least (2 :times)))
        (do
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)))

;; interaction based testing, expect at least 2 interactions
(expect (interaction
         (spit "/tmp/hello-world" "some data" :append true)
         (at-least (2 :times)))
        (do
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)
          (spit "/tmp/hello-world" "some data" :append true)))

;; redef state within the context of a test
(expect :atom
        (do
          (reset! success.success-examples-src/an-atom "atom")
          (redef-state [success.success-examples-src]
                       (reset! success.success-examples-src/an-atom :atom)
                       @success.success-examples-src/an-atom)))

(expect "atom"
        (do
          (reset! success.success-examples-src/an-atom "atom")
          (redef-state [success.success-examples-src]
                       (reset! success.success-examples-src/an-atom :atom))
          @success.success-examples-src/an-atom))

;; use expect-let to share a value between the actual and expected forms
(expect-let [x 2]
            (* x x) (+ x x))

;; use freeze-time to set the current time while a test is running
(expect-let [now (DateTime.)]
            (freeze-time now (DateTime.))
            (freeze-time now (DateTime.)))

;; freeze-time only affects wrapped forms
(expect (not= (DateTime. 1)
              (do
                (freeze-time (DateTime. 1))
                (DateTime.))))

;; freeze-time resets the frozen time even when an exception occurs
(expect (not= (DateTime. 1)
              (do
                (try
                  (freeze-time (DateTime. 1)
                               (throw (RuntimeException. "test finally")))
                  (catch Exception e))
                (DateTime.))))

;; use context to limit the number of indentions while using redef-state, with-redefs or freeze-time
(expect-let [now (DateTime.)]
            (interaction (println "trades" now))
            (context [:redef-state [success.success-examples-src]
                      :with-redefs [vector identity
                                    spit no-op]
                      :freeze-time now]
                     (spit now)
                     (println "trades" (vector (DateTime.)))))

;; ensure equality matching where possible
(expect no-op no-op)
(expect java.util.AbstractMap java.util.HashMap)
(expect #"a" #"a")
(expect RuntimeException RuntimeException)
