# expectations

> because less is more

expectations is a minimalist's testing framework

 *  simply require expectations and your tests will be run on JVM shutdown.
 *  what you are testing is inferred from the expected and actual types
 *  stacktraces are trimmed of clojure library lines
 *  focused error & failure messages

## Credit

Expectations is based on clojure.test. clojure.test is distributed under the Eclipse license, with
ownership assigned to Rich Hickey.

## Getting Started

Expectations is fairly light-weight, and so is distribution. To use expectations, copy expectations.clj into your project. You should be able to run the example below once you've added expectations to your project.

By default the tests run on JVM shutdown, so all you need to do is run your clj file and you should see the expectations output. 

(running your clj should be similar to: 
  `java -cp $CLOJURE_JAR:.: clojure.main -i examples.clj`)

If you can run the examples, you can start running your own tests.

If you want to disable running the tests on shutdown all you need to do is call: `disable-run-on-shutdown`

It makes sense to disable running tests on shutdown if you want to explicitly call the `run-all-tests` function when you want your tests run. For example, I've written a JUnit test runner that runs all the tests and provides output to IntelliJ. In that case, you'll want to run the tests explicitly and disable the shutdown hook.

However, the vast majority of the time, allowing the framework to run the tests for you is the simplest option.

## Success Examples
<pre>(ns examples
  (:use expectations))

;; number equality
(expect 1 (do 1))

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

;; val in list
(expect :foo (in (conj [:bar] :foo)))

;; expect boolean
(expect (empty? (list)))

;; multiple expects with form
(given [x y] (expect x (+ y y))
	4 2
	6 3
	12 6)

(given [x y] (expect 10 (+ x y))
	4 6
	6 4
	12 -2)

(given [x y] (expect x (in y))
	:a #{:a :b}
	{:a :b} {:a :b :c :d})

(given [x y] (expect (x y))
	nil? nil
	fn? +
	empty? [])</pre>

## Failure Examples
<pre>failure in (expectations_failures_test.clj:24)
      raw: (expect [1 2 3 2 4] [3 2 1 3])
   result: [1 2 3 2 4] does not equal [3 2 1 3]
  act-msg: 4 are in expected, but not in actual
  message: expected is larger than actual

failure in (expectations_failures_test.clj:33)
      raw: (expect foo (str "boo" "fo" "ar"))
   result: regex #"foo" not found in "boofoar"

failure in (expectations_failures_test.clj:54)
      raw: (expect (empty? (list 1)))
   result: false

failure in (expectations_failures_test.clj:39)
      raw: (expect String 1)
   result: 1 is not an instance of class java.lang.String

failure in (expectations_failures_test.clj:10)
      raw: (expect (one) 1)
  exp-msg: exception in expected: (one)
    threw: class java.lang.ArithmeticException-Divide by zero
           expectations_test$two (expectations_failures_test.clj:4)
           expectations_test$one (expectations_failures_test.clj:5)
           expectations_test$test226$fn__227 (expectations_failures_test.clj:10)
           expectations_test$test226 (expectations_failures_test.clj:10)


failure in (expectations_failures_test.clj:48)
      raw: (expect foo (in (conj ["bar"] :foo)))
   result: value "foo" not found in ["bar" :foo]

failure in (expectations_failures_test.clj:36)
      raw: (expect ArithmeticException (/ 12 12))
   result: (/ 12 12) did not throw ArithmeticException

failure in (expectations_failures_test.clj:12)
      raw: (expect (one))
  act-msg: exception in actual: (one)
    threw: class java.lang.ArithmeticException-Divide by zero
           expectations_test$two (expectations_failures_test.clj:4)
           expectations_test$one (expectations_failures_test.clj:5)
           expectations_test$test234$fn__237 (expectations_failures_test.clj:12)
           expectations_test$test234 (expectations_failures_test.clj:12)


failure in (expectations_failures_test.clj:57)
      raw: (expect 6 (+ 4 4))
   result: 6 does not equal 8

failure in (expectations_failures_test.clj:18)
      raw: (expect foos (identity "foo"))
   result: "foos" does not equal "foo"

failure in (expectations_failures_test.clj:61)
      raw: (expect 10 (+ 6 3))
   result: 10 does not equal 9

failure in (expectations_failures_test.clj:57)
      raw: (expect 12 (+ 12 12))
   result: 12 does not equal 24

failure in (expectations_failures_test.clj:15)
      raw: (expect 1 (identity 2))
   result: 1 does not equal 2

failure in (expectations_failures_test.clj:42)
      raw: (expect {:foos 1, :cat 5} (in {:foo 1, :cat 4}))
   result: {:foos 1, :cat 5} are not in {:foo 1, :cat 4}
  act-msg: :foos are in expected, but not in actual
  message: :cat expected 5 but was 4

failure in (expectations_failures_test.clj:45)
      raw: (expect foos (in (conj #{:foo :bar} "cat")))
   result: key "foos" not found in #{:foo :bar "cat"}

failure in (expectations_failures_test.clj:69)
      raw: (expect (empty? [1]))
   result: false

failure in (expectations_failures_test.clj:65)
      raw: (expect {:a :z} (in {:a :b, :c :d}))
   result: {:a :z} are not in {:a :b, :c :d}
  message: :a expected :z but was :b

failure in (expectations_failures_test.clj:27)
      raw: (expect #{:foo :bar :dog :car} (conj #{} :foo :bar :cat))
   result: #{:foo :bar :dog :car} does not equal #{:foo :bar :cat}
  exp-msg: :cat are in actual, but not in expected
  act-msg: :dog, :car are in expected, but not in actual

failure in (expectations_failures_test.clj:30)
      raw: (expect [1 2] (map - [1 2]))
   result: [1 2] does not equal clojure.lang.LazySeq@3a0
  exp-msg: -2, -1 are in actual, but not in expected
  act-msg: 1, 2 are in expected, but not in actual

failure in (expectations_failures_test.clj:69)
      raw: (expect (nil? 1))
   result: false

failure in (expectations_failures_test.clj:69)
      raw: (expect (fn? 1))
   result: false

failure in (expectations_failures_test.clj:21)
      raw: (expect {:foo 2, :bar 3, :dog 3, :car 4} (assoc {} :foo 1 :bar "3" :cat 4))
   result: {:foo 2, :bar 3, :dog 3, :car 4} does not equal {:cat 4, :bar "3", :foo 1}
  exp-msg: :cat are in actual, but not in expected
  act-msg: :dog, :car are in expected, but not in actual
  message: :bar expected 3 but was "3", :foo expected 2 but was 1

failure in (expectations_failures_test.clj:61)
      raw: (expect 10 (+ 12 -20))
   result: 10 does not equal -8

failure in (expectations_failures_test.clj:51)
      raw: (expect foo (in nil))
   result: nil
  message: You must supply a list, set, or map when using (in)

failure in (expectations_failures_test.clj:8)
      raw: (expect 1 (one))
  act-msg: exception in actual: (one)
    threw: class java.lang.ArithmeticException-Divide by zero
           expectations_test$two (expectations_failures_test.clj:4)
           expectations_test$one (expectations_failures_test.clj:5)
           expectations_test$test218$fn__221 (expectations_failures_test.clj:8)
           expectations_test$test218 (expectations_failures_test.clj:8)


failure in (expectations_failures_test.clj:65)
      raw: (expect :c (in #{:a :b}))
   result: key :c not found in #{:a :b}
</pre>

## License

Copyright (c) 2010, Jay Fields
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name Jay Fields nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.