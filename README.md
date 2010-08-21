# expectations

> because less is more

expectations is a minimalist's testing framework.
 *  simply require expectations and your tests will be run on JVM shutdown.
 *  what you are testing is inferred from the expected and actual types
 *  stacktraces are trimmed of clojure library lines
 *  focused error & failure messages

## Success Examples
<pre>   ;; passing tests

   ;; number equality
   (expect 1 (inc 0))

   ;; string equality
   (expect "foo" (identity "foo"))	

   ; map equality
   (expect {:foo 1 :bar 2 :car 4} (assoc {} :foo 1 :bar 2 :car 4))

   ;; is the regex in the string
   (expect #"foo" (str "boofoo"))

   ;; does the form throw an expeted exception
   (expect ArithmeticException (/ 12 0))

   ;; verify the type of the result
   (expect String "foo")

   ;; k/v pair in map. matches subset
   (expect {:foo 1} (in {:foo 1 :cat 4}))

   ;; key in set
   (expect :foo (in (conj #{:foo :bar} :cat)))</pre>

## Failure Examples
FAIL in expectations_test.clj:17
      raw: (expect ArithmeticException (/ 12 12))
   result: (/ 12 12) did not throw ArithmeticException

FAIL in expectations_test.clj:26
      raw: (expect :fooee (in (conj #{:foo :bar} :cat)))
   result: key :fooee not found in #{:foo :bar :cat}

FAIL in expectations_test.clj:11
      raw: (expect {:afoo 1, :bar 2, :car 4} (assoc {} :foo 1 :bar 3 :car 4))
   result: {:afoo 1, :bar 2, :car 4} does not equal {:car 4, :bar 3, :foo 1}
  exp-msg: (:foo) are in actual, but not in expected
  act-msg: (:afoo) are in expected, but not in actual
  message: :bar expected 2 but was 3

FAIL in expectations_test.clj:5
      raw: (expect 2 (inc 0))
   result: 2 does not equal 1

FAIL in expectations_test.clj:23
      raw: (expect {:foox 1} (in {:foo 1, :cat 4}))
   result: {:foox 1} are not in {:foo 1, :cat 4}
  act-msg: (:foox) are in expected, but not in actual

FAIL in expectations_test.clj:20
      raw: (expect String 1)
   result: 1 is not an instance of String

FAIL in expectations_test.clj:14
      raw: (expect #"afoo" (str boo foo ar))
   result: regex #"afoo" not found in "boofooar"

FAIL in expectations_test.clj:8
      raw: (expect afoo (identity foo))
   result: afoo does not equal foo

ERROR in expectations_test.clj:5
      raw: (expect 2 (/ 12 0))
    threw:  java.lang.ArithmeticException - Divide by zero
    expectations_test$test152.invoke (expectations_test.clj:5)