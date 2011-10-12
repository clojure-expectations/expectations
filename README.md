# expectations

> because less is more

expectations is a minimalist's testing framework

 *  simply require expectations and your tests will be run on JVM shutdown.
 *  what you are testing is inferred from the expected and actual types
 *  stacktraces are trimmed of clojure library lines and java.lang lines
 *  focused error & failure messages

## Credit

Expectations is based on clojure.test. clojure.test is distributed under the Eclipse license, with
ownership assigned to Rich Hickey.

## Installing
----------

The easiest way to use expectations in your own projects is via
[Leiningen](http://github.com/technomancy/leiningen). Add the
following dependency to your project.clj file:

    [expectations "0.1.0"]

To build expectations from source, run the following commands:

    lein deps
    lein jar

## Getting Started

By default the tests run on JVM shutdown, so all you need to do is run your clj file and you should see the expectations output. 

You can test that everything is working correctly by using
expectations in a simple test.

<pre>(ns simple.test
  (:use expectations))

(expect nil? nil)</pre>

(assuming you've put your dependencies in a (relatively pathed) lib dir)

running your clj should be similar to:
`java -cp "lib/*" clojure.main -i /path/to/your/simple/test.clj`

You can run the examples in expectations with:
`java -cp "lib/*" clojure.main -i /path/to/expectations/test/clojure/success/success_examples.clj`

If you want to disable running the tests on shutdown all you need to do is call: `disable-run-on-shutdown`

It makes sense to disable running tests on shutdown if you want to explicitly call the `run-all-tests` function when you want your tests run. For example, I've written a JUnit test runner that runs all the tests and provides output to IntelliJ. In that case, you'll want to run the tests explicitly and disable the shutdown hook.

However, the vast majority of the time, allowing the framework to run
the tests for you is the simplest option.

You're now ready to start using expectations as you see fit. There's
not a ton of syntax; however, you'll probably want to take a quick
look at the various ways you can write expectations.

## Success Examples

[available here](http://github.com/jaycfields/expectations/blob/master/test/clojure/success/success_examples.clj)

## Failure Examples
<pre>failure in (failure_examples.clj:8) : failure.failure-examples
           (expect 1 (one))
  act-msg: exception in actual: (one)
    threw: class java.lang.ArithmeticException - Divide by zero
           on (failure_examples.clj:4)
           on (failure_examples.clj:5)
           on (failure_examples.clj:8)

failure in (failure_examples.clj:10) : failure.failure-examples
           (expect (one) 1)
  exp-msg: exception in expected: (one)
    threw: class java.lang.ArithmeticException - Divide by zero
           on (failure_examples.clj:4)
           on (failure_examples.clj:5)
           on (failure_examples.clj:10)

failure in (failure_examples.clj:13) : failure.failure-examples
           (expect 1 (identity 2))
           expected: 1 
                was: 2

failure in (failure_examples.clj:16) : failure.failure-examples
           (expect foos (identity "foo"))
           expected: "foos" 
                was: "foo"

failure in (failure_examples.clj:19) : failure.failure-examples
           (expect {:foo 2, :bar 3, :dog 3, :car 4}
                   (assoc {} :foo 1 :bar "3" :cat 4))
           expected: {:foo 2, :bar 3, :dog 3, :car 4} 
                was: {:cat 4, :bar "3", :foo 1}
 
           :cat with val 4 is in actual, but not in expected
           :dog with val 3 is in expected, but not in actual
           :car with val 4 is in expected, but not in actual
           :bar expected: 3
                     was: "3"
           :foo expected: 2
                     was: 1

failure in (failure_examples.clj:22) : failure.failure-examples
           (expect [1 2 3 2 4] [3 2 1 3])
           expected: [1 2 3 2 4] 
                was: [3 2 1 3]
 
           4 are in expected, but not in actual
           expected is larger than actual

failure in (failure_examples.clj:25) : failure.failure-examples
           (expect #{:foo :bar :dog :car} (conj #{} :foo :bar :cat))
           expected: #{:foo :bar :dog :car} 
                was: #{:foo :bar :cat}
 
           :cat are in actual, but not in expected
           :dog, :car are in expected, but not in actual

failure in (failure_examples.clj:28) : failure.failure-examples
           (expect [1 2] (map - [1 2]))
           expected: [1 2] 
                was: (-1 -2)
 
           -2, -1 are in actual, but not in expected
           1, 2 are in expected, but not in actual

failure in (failure_examples.clj:31) : failure.failure-examples
           (expect foo (str "boo" "fo" "ar"))
           regex #"foo" not found in "boofoar"

failure in (failure_examples.clj:34) : failure.failure-examples
           (expect ArithmeticException (/ 12 12))
           (/ 12 12) did not throw ArithmeticException

failure in (failure_examples.clj:37) : failure.failure-examples
           (expect String 1)
           1 is not an instance of class java.lang.String

failure in (failure_examples.clj:40) : failure.failure-examples
           (expect {:foos 1, :cat 5} (in {:foo 1, :cat 4}))
           expected: {:foos 1, :cat 5} 
                 in: {:foo 1, :cat 4}
 
           :foos with val 1 is in expected, but not in actual
           :cat expected: 5
                     was: 4

failure in (failure_examples.clj:43) : failure.failure-examples
           (expect foos (in (conj #{:foo :bar} "cat")))
           key "foos" not found in #{:foo :bar "cat"}

failure in (failure_examples.clj:46) : failure.failure-examples
           (expect foo (in (conj ["bar"] :foo)))
           value "foo" not found in ["bar" :foo]

failure in (failure_examples.clj:49) : failure.failure-examples
           (expect foo (in nil))
           You supplied: nil
 
           You must supply a list, set, or map when using (in)

failure in (failure_examples.clj:52) : failure.failure-examples
           (expect empty? (list 1))
           (1) is not empty?

failure in (failure_examples.clj:55) : failure.failure-examples
           (expect #{1 9} #{1 Double/NaN})
           expected: #{1 9} 
                was: #{NaN 1}
 
           NaN are in actual, but not in expected
           9 are in expected, but not in actual

failure in (failure_examples.clj:58) : failure.failure-examples
           (expect Double/NaN (in #{1}))
           key NaN not found in #{1}

failure in (failure_examples.clj:61) : failure.failure-examples
           (expect [1 Double/NaN] [1])
           expected: [1 NaN] 
                was: [1]
 
           NaN are in expected, but not in actual
           expected is larger than actual

failure in (failure_examples.clj:64) : failure.failure-examples
           (expect Double/NaN (in [1]))
           value NaN not found in [1]

failure in (failure_examples.clj:67) : failure.failure-examples
           (expect {:a Double/NaN, :b {:c 9}} {:a Double/NaN, :b {:c Double/NaN}})
           expected: {:a NaN, :b {:c 9}} 
                was: {:a NaN, :b {:c NaN}}
 
           :b {:c expected: 9
                       was: NaN

failure in (failure_examples.clj:70) : failure.failure-examples
           (expect {:a Double/NaN, :b {:c 9}} (in {:a Double/NaN, :b {:c Double/NaN}, :d "other stuff"}))
           expected: {:a NaN, :b {:c 9}} 
                 in: {:a NaN, :b {:c NaN}, :d "other stuff"}
 
           :b {:c expected: 9
                       was: NaN

failure in (failure_examples.clj:73) : failure.failure-examples
           (expect 6 (+ 4 4))
           expected: 6 
                was: 8

failure in (failure_examples.clj:73) : failure.failure-examples
           (expect 12 (+ 12 12))
           expected: 12 
                was: 24

failure in (failure_examples.clj:77) : failure.failure-examples
           (expect 10 (+ 6 3))
           expected: 10 
                was: 9

failure in (failure_examples.clj:77) : failure.failure-examples
           (expect 10 (+ 12 -20))
           expected: 10 
                was: -8

failure in (failure_examples.clj:81) : failure.failure-examples
           (expect :c (in #{:a :b}))
           key :c not found in #{:a :b}

failure in (failure_examples.clj:81) : failure.failure-examples
           (expect {:a :z} (in {:a :b, :c :d}))
           expected: {:a :z} 
                 in: {:a :b, :c :d}
 
           :a expected: :z
                   was: :b

failure in (failure_examples.clj:85) : failure.failure-examples
           (expect nil? 1)
           1 is not nil?

failure in (failure_examples.clj:85) : failure.failure-examples
           (expect fn? 1)
           1 is not fn?

failure in (failure_examples.clj:85) : failure.failure-examples
           (expect empty? [1])
           [1] is not empty?

failure in (failure_examples.clj:91) : failure.failure-examples
           (expect 1 (.size (java.util.ArrayList.)))
           expected: 1 
                was: 0

failure in (failure_examples.clj:91) : failure.failure-examples
           (expect false (.isEmpty (java.util.ArrayList.)))
           expected: false 
                was: true

failure in (failure_examples.clj:97) : failure.failure-examples
           (expect 0 (first [1 2 3]))
           expected: 0 
                was: 1

failure in (failure_examples.clj:97) : failure.failure-examples
           (expect 4 (last [1 2 3]))
           expected: 4 
                was: 3

failure in (failure_examples.clj:102) : failure.failure-examples
           (expect 99 (:a {:a 2, :b 4}))
           expected: 99 
                was: 2

failure in (failure_examples.clj:102) : failure.failure-examples
           (expect 100 (:b {:a 2, :b 4}))
           expected: 100 
                was: 4

failure in (failure_examples.clj:108) : failure.failure-examples
           (expect {:z 1, :a 9, :b {:c Double/NaN, :d 1, :e 2, :f {:g 10, :i 22}}}
                   {:x 1, :a Double/NaN, :b {:c Double/NaN, :d 2, :e 4, :f {:g 11, :h 12}}})
           expected: {:z 1, :a 9, :b {:c NaN, :d 1, :e 2, :f {:g 10, :i 22}}} 
                was: {:x 1, :a NaN, :b {:c NaN, :d 2, :e 4, :f {:g 11, :h 12}}}
 
           :x with val 1 is in actual, but not in expected
           :b {:f {:h with val 12 is in actual, but not in expected
           :z with val 1 is in expected, but not in actual
           :b {:f {:i with val 22 is in expected, but not in actual
           :b {:e expected: 2
                       was: 4
           :b {:d expected: 1
                       was: 2
           :b {:f {:g expected: 10
                           was: 11
           :a expected: 9
                   was: NaN
</pre>

## License

Copyright (c) 2010, Jay Fields
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name Jay Fields nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
