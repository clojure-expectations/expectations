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

http://github.com/jaycfields/expectations/blob/master/test/clojure/success/success_examples.clj

## Failure Examples
<pre>failure in (failure_examples.clj:8) : failure.failure-examples
      raw: (expect 1 (one))
  act-msg: exception in actual: (one)
    threw: class java.lang.ArithmeticException-Divide by zero
           failure.failure_examples$two__375 (failure_examples.clj:4)
           failure.failure_examples$one__378 (failure_examples.clj:5)
           failure.failure_examples$G__381__382$fn__387 (failure_examples.clj:8)
           failure.failure_examples$G__381__382 (failure_examples.clj:8)

failure in (failure_examples.clj:10) : failure.failure-examples
      raw: (expect (one) 1)
  exp-msg: exception in expected: (one)
    threw: class java.lang.ArithmeticException-Divide by zero
           failure.failure_examples$two__375 (failure_examples.clj:4)
           failure.failure_examples$one__378 (failure_examples.clj:5)
           failure.failure_examples$G__393__394$fn__396 (failure_examples.clj:10)
           failure.failure_examples$G__393__394 (failure_examples.clj:10)

failure in (failure_examples.clj:12) : failure.failure-examples
      raw: (expect (one))
  act-msg: exception in actual: (one)
    threw: class java.lang.ArithmeticException-Divide by zero
           failure.failure_examples$two__375 (failure_examples.clj:4)
           failure.failure_examples$one__378 (failure_examples.clj:5)
           failure.failure_examples$G__405__406$fn__411 (failure_examples.clj:12)
           failure.failure_examples$G__405__406 (failure_examples.clj:12)

failure in (failure_examples.clj:15) : failure.failure-examples
      raw: (expect 1 (identity 2))
   result: 1 does not equal 2

failure in (failure_examples.clj:18) : failure.failure-examples
      raw: (expect foos (identity "foo"))
   result: "foos" does not equal "foo"

failure in (failure_examples.clj:21) : failure.failure-examples
      raw: (expect {:foo 2, :bar 3, :dog 3, :car 4} (assoc {} :foo 1 :bar "3" :cat 4))
   result: {:foo 2, :bar 3, :dog 3, :car 4} are not in {:cat 4, :bar "3", :foo 1}
  exp-msg: :cat is in actual, but not in expected
  act-msg: :dog is in expected, but not in actual
           :car is in expected, but not in actual
  message: :bar expected 3 but was "3"
           :foo expected 2 but was 1

failure in (failure_examples.clj:24) : failure.failure-examples
      raw: (expect [1 2 3 2 4] [3 2 1 3])
   result: [1 2 3 2 4] does not equal [3 2 1 3]
  act-msg: 4 are in expected, but not in actual
  message: expected is larger than actual

failure in (failure_examples.clj:27) : failure.failure-examples
      raw: (expect #{:foo :bar :dog :car} (conj #{} :foo :bar :cat))
   result: #{:foo :bar :dog :car} does not equal #{:foo :bar :cat}
  exp-msg: :cat are in actual, but not in expected
  act-msg: :dog, :car are in expected, but not in actual

failure in (failure_examples.clj:30) : failure.failure-examples
      raw: (expect [1 2] (map - [1 2]))
   result: [1 2] does not equal clojure.lang.LazySeq@3a0
  exp-msg: -2, -1 are in actual, but not in expected
  act-msg: 1, 2 are in expected, but not in actual

failure in (failure_examples.clj:33) : failure.failure-examples
      raw: (expect foo (str "boo" "fo" "ar"))
   result: regex #"foo" not found in "boofoar"

failure in (failure_examples.clj:36) : failure.failure-examples
      raw: (expect ArithmeticException (/ 12 12))
   result: (/ 12 12) did not throw ArithmeticException

failure in (failure_examples.clj:39) : failure.failure-examples
      raw: (expect String 1)
   result: 1 is not an instance of class java.lang.String

failure in (failure_examples.clj:42) : failure.failure-examples
      raw: (expect {:foos 1, :cat 5} (in {:foo 1, :cat 4}))
   result: {:foos 1, :cat 5} are not in {:foo 1, :cat 4}
  act-msg: :foos is in expected, but not in actual
  message: :cat expected 5 but was 4

failure in (failure_examples.clj:45) : failure.failure-examples
      raw: (expect foos (in (conj #{:foo :bar} "cat")))
   result: key "foos" not found in #{:foo :bar "cat"}

failure in (failure_examples.clj:48) : failure.failure-examples
      raw: (expect foo (in (conj ["bar"] :foo)))
   result: value "foo" not found in ["bar" :foo]

failure in (failure_examples.clj:51) : failure.failure-examples
      raw: (expect foo (in nil))
   result: nil
  message: You must supply a list, set, or map when using (in)

failure in (failure_examples.clj:54) : failure.failure-examples
      raw: (expect (empty? (list 1)))
   result: false

failure in (failure_examples.clj:57) : failure.failure-examples
      raw: (expect #{1 9} #{1 Double/NaN})
   result: #{1 9} does not equal #{NaN 1}
  exp-msg: NaN are in actual, but not in expected
  act-msg: 9 are in expected, but not in actual

failure in (failure_examples.clj:60) : failure.failure-examples
      raw: (expect Double/NaN (in #{1}))
   result: key NaN not found in #{1}

failure in (failure_examples.clj:63) : failure.failure-examples
      raw: (expect [1 Double/NaN] [1])
   result: [1 NaN] does not equal [1]
  act-msg: NaN are in expected, but not in actual
  message: expected is larger than actual

failure in (failure_examples.clj:66) : failure.failure-examples
      raw: (expect Double/NaN (in [1]))
   result: value NaN not found in [1]

failure in (failure_examples.clj:69) : failure.failure-examples
      raw: (expect {:a Double/NaN, :b {:c 9}} {:a Double/NaN, :b {:c Double/NaN}})
   result: {:a NaN, :b {:c 9}} are not in {:a NaN, :b {:c NaN}}
  message: :b {:c expected 9 but was NaN

failure in (failure_examples.clj:72) : failure.failure-examples
      raw: (expect {:a Double/NaN, :b {:c 9}} (in {:a Double/NaN, :b {:c Double/NaN}, :d "other stuff"}))
   result: {:a NaN, :b {:c 9}} are not in {:a NaN, :b {:c NaN}, :d "other stuff"}
  message: :b {:c expected 9 but was NaN

failure in (failure_examples.clj:75) : failure.failure-examples
      raw: (expect 6 (+ 4 4))
   result: 6 does not equal 8

failure in (failure_examples.clj:75) : failure.failure-examples
      raw: (expect 12 (+ 12 12))
   result: 12 does not equal 24

failure in (failure_examples.clj:79) : failure.failure-examples
      raw: (expect 10 (+ 6 3))
   result: 10 does not equal 9

failure in (failure_examples.clj:79) : failure.failure-examples
      raw: (expect 10 (+ 12 -20))
   result: 10 does not equal -8

failure in (failure_examples.clj:83) : failure.failure-examples
      raw: (expect :c (in #{:a :b}))
   result: key :c not found in #{:a :b}

failure in (failure_examples.clj:83) : failure.failure-examples
      raw: (expect {:a :z} (in {:a :b, :c :d}))
   result: {:a :z} are not in {:a :b, :c :d}
  message: :a expected :z but was :b

failure in (failure_examples.clj:87) : failure.failure-examples
      raw: (expect (nil? 1))
   result: false

failure in (failure_examples.clj:87) : failure.failure-examples
      raw: (expect (fn? 1))
   result: false

failure in (failure_examples.clj:87) : failure.failure-examples
      raw: (expect (empty? [1]))
   result: false

failure in (failure_examples.clj:93) : failure.failure-examples
      raw: (expect 1 (.size (java.util.ArrayList.)))
   result: 1 does not equal 0

failure in (failure_examples.clj:93) : failure.failure-examples
      raw: (expect false (.isEmpty (java.util.ArrayList.)))
   result: false does not equal true

failure in (failure_examples.clj:99) : failure.failure-examples
      raw: (expect 0 (first [1 2 3]))
   result: 0 does not equal 1

failure in (failure_examples.clj:99) : failure.failure-examples
      raw: (expect 4 (last [1 2 3]))
   result: 4 does not equal 3

failure in (failure_examples.clj:104) : failure.failure-examples
      raw: (expect 99 (:a {:a 2, :b 4}))
   result: 99 does not equal 2

failure in (failure_examples.clj:104) : failure.failure-examples
      raw: (expect 100 (:b {:a 2, :b 4}))
   result: 100 does not equal 4

failure in (failure_examples.clj:110) : failure.failure-examples
      raw: (expect {:z 1, :a 9, :b {:c Double/NaN, :d 1, :e 2, :f {:g 10, :i 22}}} {:x 1, :a Double/NaN, :b {:c Double/NaN, :d 2, :e 4, :f {:g 11, :h 12}}})
   result: {:z 1, :a 9, :b {:c NaN, :d 1, :e 2, :f {:g 10, :i 22}}} are not in {:x 1, :a NaN, :b {:c NaN, :d 2, :e 4, :f {:g 11, :h 12}}}
  exp-msg: :x is in actual, but not in expected
           :b {:f {:h is in actual, but not in expected
  act-msg: :z is in expected, but not in actual
           :b {:f {:i is in expected, but not in actual
  message: :b {:e expected 2 but was 4
           :b {:d expected 1 but was 2
           :b {:f {:g expected 10 but was 11
           :a expected 9 but was NaN
</pre>

## License

Copyright (c) 2010, Jay Fields
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name Jay Fields nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.