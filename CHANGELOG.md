# Changes to expectations in Version 2.0

## CONTENTS

## 1 new and improved features

### 1.1 side-effects

expectations 2.0 introduces the 'side-effets macro, which allows you to
capture arguments to functions you specify. Previous to version 2.0
behavior was often tested using the 'interaction macro. expectations 2.0
removes the 'interaction macro and embraces the idea of verifying
interactions at the data level, using the same type of comparison that
is used for all other data.

Examples:

    (expect [["/tmp/hello-world" "some data" :append true]
             ["/tmp/hello-world" "some data" :append true]]
      (side-effects [spit]
                (spit "/tmp/hello-world" "some data" :append true)
                (spit "/tmp/hello-world" "some data" :append true)))

In the above example, you specify that spit is a side effect
fn, and the 'side-effects macro will return a list of all calls
made, with the arguments used in the call. The above example
uses simple equality for verification.

    (expect empty?
      (side-effects [spit] "spit never called"))

The above example demonstrates how you can use non-equality to
to verify the data returned.

    (expect ["/tmp/hello-world" "some data" :append true]
      (in (side-effects [spit]
                    (spit "some other stuff" "xy")
                    (spit "/tmp/hello-world" "some data" :append true))))

Immediately above is an example of combining 'side-effects with 'in
for a more concise test. Here we're testing that the expected data
will exist somewhere within the list returned by 'side-effects

### 1.2 more, more->, & more-of

expectations has always given you the ability to test against an
arbitrary fn, similar to the example below.

    (expect nil? nil)

The ability to specify any fn is powerful, but it doesn't always give
you the most descriptive failure messages. In expectations 2.0 we
introduce the 'more, 'more->, & 'more-of macros, which are designed
to allow you to expect more of your actual values.

Below is a simple example of using the more macro.

    (expect (more vector? not-empty) [1 2 3])

As you can see from the above example, we're simply expecting that
the actual value '[1 2 3] is both a 'vector? and 'not-empty. The
'more macro is great when you want to test a few 1 arg fns; however,
I expect you'll more often find yourself reaching for 'more-> and
'more-of.

The 'more-> macro is used for threading the actual value and
comparing the result to an expected value. Below is a simple example
of using 'more to pull values out of a vector and test their equality.

    (expect (more-> 1 first
                    3 last)
      [1 2 3])

The 'more-> macro threads using -> (thread-first), so you're able
to put any form you'd like in the actual transformation.

    (expect (more-> 2 (-> first (+ 1))
                    3 last)
      [1 2 3])

Finally, 'more-> can be very helpful for testing various kv pairs
within a map, or various Java fields.

    (expect (more-> 0 .size
                    true .isEmpty)
       (java.util.ArrayList.))

    (expect (more-> 2 :a
                    4 :b)
       {:a 2 :b 4})

Threading is great work, if you can get it. For the times when
you need to name your actual value, 'more-of should do the trick.
The following example demonstrates how to name your actual value
and then specify a few expectations.

    (expect (more-of x
                     vector? x
                     1 (first x))
      [1 2 3])

If you've ever found yourself wishing you had destructuring in
clojure.test/are or expectations/given, you're not alone. The
good news is, 'more-of supports any destructuring you want to
give it.

    (expect (more-of [x :as all]
                     vector? all
                     1 x)
      [1 2 3])

### 1.3 combining side-effects and more-of

It's fairly common to expect some behavior where you know the
exact values for some of the args, and you have something more
general in mind for the additional args. By combining 'side-effects
and 'more-of you can easily great a call into it's args and verify
as many as you care to verify.

    (expect (more-of [path data action {:keys [a c]}]
                     String path
                     #"some da" data
                     keyword? action
                     :b a
                     :d c)
      (in (side-effects [spit]
            (spit "/tmp/hello-world" "some data" :append {:a :b :c :d :e :f}))))

The above test is a bit much to swallow at first glance; however,
it's actually very straightforward once you've gotten used to the
'more-of syntax. In the above example the 'spit fn is called with
the args "/tmp/hello-world", "some data" :append {:a :b :c :d :e :f}.
Using 'more-of, we destructure those args, and expect them
individually. The path arg is expected to be of type String. The
data arg is expected to be a string that matches the regex
#"some da". The action is expected to be a 'keyword?. Finally,
the options map is destructured to it's :a and :c values, and
equality expeted.

### 1.4 from-each

It's common to expect something from a list of actual values.
Traditionally 'given was used to generate many tests from one
form. Unfortunately 'given suffered from many issues: no
ability to destructure values, failure line numbers were
almost completely useless, and little visibility into what
the problem was when a failure did occur.

In expectations 2.0 'from-each was introduced to provide
a more powerful syntax as well as more helpful failure
messages.

Below you can see a very simple expectation that verifies
each of the elements of a vector is a String.

    (expect String
      (from-each [letter ["a" "b" "c"]]
        letter))

Hopefully the syntax of 'from-each feels very familiar, it's
been written to handle the same options as 'for and 'doseq -
:let and :when.

    (expect odd? (from-each [num [1 2 3]
                             :when (not= num 2)]
                   num))

    (expect odd? (from-each [num [1 2 3]
                             :when (not= num 2)
                             :let [numinc1 (inc num)]]
                   (inc numinc1)))

While 'from-each is helpful in creating concise tests, I
actually find it's most value when a test fails. If you
take the above test and remove the :when, you would have
the test below.

    (expect odd? (from-each [num [1 2 3]
                             :let [numinc1 (inc num)]]
                   (inc numinc1)))

The above test would definitely fail, but it's not
immediately obvious what the issue is. However, the failure
message should quickly lead you to the underlying issue.

```clojure
    failure in (success_examples.clj:206) : success.success-examples
    (expect
     odd?
     (from-each [num [1 2 3] :let [numinc1 (inc num)]] (inc numinc1)))

               the list: (3 4 5)

    (expect odd? (inc numinc1))

                 locals num: 2
                        numinc1: 3
               4 is not odd?
```

As you can see above, when 'from-each fails it will give you
values of every var defined within the 'from-each bindings. As
a result, it's fairly easy to find the combination of vars that
led to a failing test.

## 2 changed

### 2.1 custom diff syntax removed

expectations was originally written before clojure.data/diff
existed, and defined it's own syntax for printing diffed data
between actual and expected. Now that there's a standard, it
no longer makes sense to define a custom syntax. All error
messages that previously used expectations custom diff syntax
have been converted to simply use clojure.data/diff result maps.

Below is an example failure message that utilizes
clojure.data/diff for reporting the inconsistencies.

    failure in (failure_examples.clj:23) : failure.failure-examples
    (expect
     {:foo 1, :bar 3, :dog 3, :car 4}
     (assoc {} :foo 1 :bar "3" :cat 4))

               expected: {:foo 1, :bar 3, :dog 3, :car 4}
                    was: {:cat 4, :bar "3", :foo 1}

               in expected, not actual: {:car 4, :dog 3, :bar 3}
               in actual, not expected: {:cat 4, :bar "3"}

## 3 removed

### 3.1 given

As I said above, 'given suffered from many issues: no
ability to destructure values, failure line numbers were
almost completely useless, and little visibility into what
the problem was when a failure did occur. I personally found
given expectations to often be the hardest to maintain, and
often I felt they were completely unmaintainable.

As a result, 'given has been replaced with 'from-each, 'more,
'more->, & 'more-of. I've converted a few codebases over, and
I've found the 'more-* or 'from-each post conversion test to
be far more maintainable.

### 3.2 function interaction tests

expectations 2.0 abandons behavior based testing and the
interaction syntax. Existing tests relying on interaction
should be easily convertable to side-effects.

### 3.3 java mock interaction tests

java mock interaction tests never really fit well within
expectations, and the removal of function interaction
tests caused us to remove the java mock support as well.

### 3.4 Double/NaN support

The original project using expectations made heavy usage of
Double/NaN, and the original version of expectations did it's
best to hide the fact that Double/NaN isn't = to Double/NaN.
Unfortunately, over time this hiding became confusing and
complicated to maintain. In expectations 2.0 we gave up on this
fight and removed Double/NaN support.

If you find yourself fighting the NaN battle, I'd suggest
writing a helper method and simply using that in your tests.
Here are a few [examples](https://gist.github.com/jaycfields/9050825)
