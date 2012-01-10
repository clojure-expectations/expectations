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

    [expectations "1.2.1"] ; clojure 1.2
    [expectations "1.3.3"] ; clojure 1.3

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

Running your clj should be similar to:
`java -cp "lib/*" clojure.main -i /path/to/your/simple/test.clj`

At this point you should see output similar to:

<pre>Ran 1 tests containing 1 assertions in 5 msecs
0 failures, 0 errors.</pre>

You can run the examples in expectations with:
`java -cp "lib/*" clojure.main -i /path/to/expectations/test/clojure/success/success_examples.clj`

You can also run expectations using lein if you install [lein-expectations](https://github.com/gar3thjon3s/lein-expectations).
Use [lein-autoexpect](https://github.com/jakemcc/lein-autoexpect) to automatically
run when your Clojure source changes.

You're now ready to start using expectations as you see fit. There's
not a ton of syntax; however, you'll probably want to take a quick
look at the various ways you can write expectations.

## Success Examples

[using 'except', 'given' and 'expanding'](test/clojure/success/success_examples.clj)

[using 'scenario' and 'stubbing'](test/clojure/success/success_examples.clj)

## Detailed Examples with Discussion

[Unit Testing Examples - Part One (Introduction)](http://blog.jayfields.com/2011/11/clojure-expectations-introduction.html)

[Unit Testing Examples - Part Two (Non-Equality)](http://blog.jayfields.com/2011/11/clojure-non-equality-expectations.html)

[Unit Testing Examples - Part Three (Using Values)](http://blog.jayfields.com/2011/11/clojure-expectations-with-values-in.html)

[Unit Testing Examples - Part Four (Testing Double/NaN)](http://blog.jayfields.com/2011/11/clojure-expectations-and-doublenan.html)

[Unit Testing Examples - Part Five (Removing Duplication with 'given')](http://blog.jayfields.com/2011/11/clojure-expectations-removing.html)

[Unit Testing Examples - Part Six (Wrap Up)](http://blog.jayfields.com/2011/11/clojure-expectations-unit-testing-wrap.html)

## License

Copyright (c) 2010, Jay Fields
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name Jay Fields nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
