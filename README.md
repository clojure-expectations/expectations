# expectations

> where less is more

expectations is a minimalist's testing framework

 *  simply require expectations and your tests will be run on JVM shutdown.
 *  what you are testing is inferred from the expected and actual types
 *  stacktraces are trimmed of clojure library lines and java.lang lines
 *  focused error & failure messages

----------

## 10 second example (inferred testing demo)

```clojure
;; expectations uses the format (expect expected actual) for all tests 
;; (equality, expected exception, regex matching, interactions, etc).

;; use expectations to test equality
(expect 1 1)
(expect "foo" "foo")

;; test if the regex is in a string
(expect #"foo" "boofooar")

;; does the form throw an expected exception
(expect ArithmeticException (/ 12 0))

;; expect a value in a collection (k/v for maps)
(expect {:foo 1} (in {:foo 1 :cat 4}))
(expect :foo (in #{:foo :bar}))
(expect :foo (in [:bar :foo]))

;; expect a function to return a truthy value given the actual argument
(expect empty? (list))
```

## Installing


The easiest way to use expectations in your own projects is via
[Leiningen](http://github.com/technomancy/leiningen). Add the
following dependency to your project.clj file:

```clojure
[expectations "1.2.1"] ; clojure 1.2
[expectations "1.4.33"] ; clojure 1.3
```

To build expectations from source, run the following commands:

```bash
$ lein deps
$ lein jar
```

## Getting Started With Leiningen

Add simple_test.clj to your test directory

```clojure
(ns simple-test
  (:use expectations))

(expect nil? nil)
```

expectations integrates with Leiningen via [lein-expectations](https://github.com/gar3thjon3s/lein-expectations).

### Usage for lein-expectations:

Declare `lein-expectations` in `project.clj`:

```clojure
:plugins [[lein-expectations "0.0.7"]]
```

To run all your tests:

```bash
$ lein expectations
```

You can also use [lein-autoexpect](https://github.com/jakemcc/lein-autoexpect) to automatically run expectations when your Clojure source changes.

## Getting Started With Emacs

Add simple_test.clj to your test directory

```clojure
(ns simple-test
  (:use expectations))

(expect nil? nil)
```

Follow the directions available on [expectations-mode](https://github.com/gar3thjon3s/expectations-mode)

## Getting Started With IntelliJ

Create a directory and mark it as a Test Source. [more info](http://www.jetbrains.com/idea/webhelp/configuring-folders-within-a-content-root.html#mark)
For example, you can create proj/test/java and mark 'java' as a test source. Then you also create proj/test/clojure and also mark that as a test source directory.

If you want all of your tests to run in JUnit all you need to do is implement ExpectationsTestRunner.TestSource.
The following example is what I use to run all the tests in expectations with JUnit.

```java
import expectations.junit.ExpectationsTestRunner;
import org.junit.runner.RunWith;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class ClojureTests implements ExpectationsTestRunner.TestSource{

    public String testPath() {
        // return the path to your root test dir here
        return "test/root/dir";
    }
}
```

Create ClojureTests.java in your proj/test/java directory. Next you can create sample-test in proj/test/clojure.

```clojure
(ns simple-test
  (:use expectations))

(expect nil? nil)
```

That's it, you can now use the JUnit Test Runner built into IntelliJ to execute your expectations.

## Getting Started Otherwise

By default the tests run on JVM shutdown, so all you need to do is run your clj file and you should see the expectations output.

You can test that everything is working correctly by using
expectations in a simple test.

```clojure
(ns simple.test
  (:use expectations))

(expect nil? nil)
```

(assuming you've put your dependencies in a (relatively pathed) lib dir)

Running your clj should be similar to:
```bash
$ java -cp "lib/*" clojure.main -i /path/to/your/simple/test.clj
```

At this point you should see output similar to:

<pre>Ran 1 tests containing 1 assertions in 5 msecs
0 failures, 0 errors.</pre>

You can run the examples in expectations with:
```bash
$ java -cp "lib/*" clojure.main -i /path/to/expectations/test/clojure/success/success_examples.clj
```

## Examples

You're now ready to start using expectations as you see fit. There's not a ton of syntax; however, you'll probably want to take a quick look at the various ways you can write expectations: [Syntax Examples](http://github.com/jaycfields/expectations/blob/master/test/clojure/success/success_examples.clj)

## Detailed Examples with Discussion

[Unit Testing Examples - Part One (Introduction)](http://blog.jayfields.com/2011/11/clojure-expectations-introduction.html)

[Unit Testing Examples - Part Two (Non-Equality)](http://blog.jayfields.com/2011/11/clojure-non-equality-expectations.html)

[Unit Testing Examples - Part Three (Using Values)](http://blog.jayfields.com/2011/11/clojure-expectations-with-values-in.html)

[Unit Testing Examples - Part Four (Testing Double/NaN)](http://blog.jayfields.com/2011/11/clojure-expectations-and-doublenan.html)

[Unit Testing Examples - Part Five (Removing Duplication with 'given')](http://blog.jayfields.com/2011/11/clojure-expectations-removing.html)

[Unit Testing Examples - Part Six (Wrap Up)](http://blog.jayfields.com/2011/11/clojure-expectations-unit-testing-wrap.html)

[Use expect-let To Share A Value Between expected And actual](http://blog.jayfields.com/2012/11/clojure-use-expect-let-to-share-value.html)

[Freezing Time Added To expectations](http://blog.jayfields.com/2012/11/clojure-freezing-time-added-to.html)

[Interaction Based Testing Added To expectations](http://blog.jayfields.com/2012/11/clojure-interaction-based-testing-added.html)

[redef-state Added To expectations](http://blog.jayfields.com/2012/10/clojure-redef-state-added-to.html)

## Colorizing

By default, expectations uses ansi escape codes to color output on non-windows environments. expectations also respects a EXPECTATIONS_COLORIZE environment variable - set the var to false if you'd like to turn off colorizing.
[more info](http://blog.jayfields.com/2012/05/clojure-expectations-colorized.html)

## Credit

Expectations is based on clojure.test. clojure.test is distributed under the Eclipse license, with
ownership assigned to Rich Hickey.

## License

Copyright (c) 2010, Jay Fields
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name Jay Fields nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
