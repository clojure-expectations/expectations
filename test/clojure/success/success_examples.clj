(ns success.success-examples
  (:use expectations erajure.core)
  (:require success.success-examples-src)
  (:import [org.joda.time DateTime]))

;; expect a truthy value
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

;; expect truthy fn return
(expect empty? (list))

;; sorted map equality
(expect (sorted-map-by > 1 :a 2 :b) (sorted-map-by > 1 :a 2 :b))

;; macro expansion
(defmacro a-macro [& args]
  `(println ~@args))

(expect '(clojure.core/println 1 2 (println 100) 3)
  (expanding (a-macro 1 2 (println 100) 3)))

(expect (more vector? not-empty) [1 2 3])

(expect (more-> 0 .size
                true .isEmpty)
  (java.util.ArrayList.))

(expect (more-> 2 (-> first (+ 1))
                3 last)
  [1 2 3])

(expect (more-> 2 :a
                4 :b)
  {:a 2 :b 4})

(expect (more-of x
                 vector? x
                 1 (first x))
  [1 2 3])

(expect ["/tmp/hello-world" "some data" :append true]
  (second (side-effects [spit]
                        (spit "some other stuff" "xy")
                        (spit "/tmp/hello-world" "some data" :append true))))

(expect empty?
  (side-effects [spit] "spit never called"))

(expect [["/tmp/hello-world" "some data" :append true]
         ["/tmp/hello-world" "some data" :append true]]
  (side-effects [spit]
                (spit "/tmp/hello-world" "some data" :append true)
                (spit "/tmp/hello-world" "some data" :append true)))

(expect ["/tmp/hello-world" "some data" :append true]
  (in (side-effects [spit]
                    (spit "some other stuff" "xy")
                    (spit "/tmp/hello-world" "some data" :append true))))

(expect (more-of [path data action flags]
                 String path
                 #"some da" data
                 keyword? action
                 {:a :b :c :d} (in flags))
  (first (side-effects [spit]
                       (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f}))))

(expect (more-of [a b] number? a)
  (from-each [x [[1 2] [1 3]]]
    x))

(expect 1
  (from-each [x [[1 2] [1 3]]]
    (in x)))

(expect (more identity not-empty)
  (in (side-effects [spit]
                    (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f}))))

(expect (more-> String (nth 0)
                #"some da" (nth 1)
                keyword? (nth 2)
                {:a :b :c :d} (-> (nth 3) (select-keys [:a :c])))
  (in (side-effects [spit]
                    (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f}))))

(expect (more-of [path data action {:keys [a c]}]
                 String path
                 #"some da" data
                 keyword? action
                 :b a
                 :d c)
  (in (side-effects [spit]
                    (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f}))))

(expect not-empty
  (side-effects [spit]
                (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f})))

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
  [now now]
  (context [:redef-state [success.success-examples-src]
            :with-redefs [spit no-op]
            :freeze-time now]
    (spit now)
    (vector now (DateTime.))))

;; ensure equality matching where possible
(expect no-op no-op)
(expect java.util.AbstractMap java.util.HashMap)
(expect #"a" #"a")
(expect RuntimeException RuntimeException)

(expect :a-rebound-val (success.success-examples-src/a-fn-to-be-rebound))

(expect String
  (from-each [letter ["a" "b" "c"]] letter))

(expect even? (from-each [num [1 2 3]
                          :let [numinc1 (inc num)
                                numinc2 (inc num)]]
                (* 10 numinc2)))

(defrecord ConstantlyTrue []
  CustomPred
  (expect-fn [e a] true)
  (expected-message [e a str-e str-a] (format "expected %s" e))
  (actual-message [e a str-e str-a] (format "actual %s" a))
  (message [e a str-e str-a] (format "%s & %s" str-e str-a)))

(expect (->ConstantlyTrue) [1 2 3 4])
