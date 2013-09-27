(ns expectations.expectations-expectations
  (:use expectations))

(expect "\n
           -- got: (hello-fn {} 3 4)
           - arg1: {}
           :a with val 1 is in expected, but not in actual
           :b with val 2 is in expected, but not in actual
           - arg2: 3
           - arg3: 4"
        (compare-args '(hello-fn {:a 1 :b 2} 2 3) "hello-fn" [{:a 1 :b 2} 2 3] [{} 3 4]))

(expect "\n
           -- got: (hello-fn 1 2 3 4)"
        (compare-args '(hello-fn 1 2) "hello-fn" [1 2] [1 2 3 4]))

(expect "\n
           -- got: (hello-fn 1 2)
           - arg3: \"no arg given\"
           - arg4: \"no arg given\""
        (compare-args '(hello-fn 1 2 3 4) "hello-fn" [1 2 3 4] [1 2]))

(expect "\n
           -- got: (hello-fn {} 3 4)
           - arg1: {}
           :a with val 1 is in expected, but not in actual
           :b with val 2 is in expected, but not in actual
           - arg3: 4"
        (compare-args '(hello-fn {:a 1 :b 2} anything 3)
                      "hello-fn" [{:a 1 :b 2} anything 3] [{} 3 4]))

(expect "\n
           -- got: (hello-fn {} 3 4)
           - arg1: {}
           :a with val 1 is in expected, but not in actual
           :b with val 2 is in expected, but not in actual"
        (compare-args '(hello-fn {:a 1 :b 2} anything&)
                      "hello-fn" [{:a 1 :b 2} anything&] [{} 3 4]))

(expect "\n
           -- got: (hello-fn 1 2)"
        (compare-args '(hello-fn)  "hello-fn" nil [1 2]))

(expect "\n
           -- got: (hello-fn)
           - arg1: \"no arg given\"
           - arg2: \"no arg given\""
        (compare-args '(hello-fn 1 2)  "hello-fn" [1 2] nil))

(expect "           result\n\n           expected-message\n           actual-message\n           message"
        (->failure-message
         {:raw "raw"
          :result ["result"]
          :expected-message "expected-message"
          :actual-message "actual-message"
          :message "message"}))

(expect "(expect raw-e raw-a)\n\n           result
\n           expected-message\n           actual-message\n           message"
        (with-redefs [show-raw-choice (constantly true)]
          (->failure-message
           {:raw '(raw-e raw-a)
            :result ["result"]
            :expected-message "expected-message"
            :actual-message "actual-message"
            :message "message"})))

(expect "(expect (whole thing) nil)\n
           the list blah
\n(expect raw-e raw-a)\n\n           result\n\n           expected-message
           actual-message\n           message
\n(expect raw-e2 raw-a2)\n\n           result2\n\n           expected-message2
           actual-message2\n           message2"
        (with-redefs [show-raw-choice (constantly true)]
          (->failure-message
           {:raw ['(whole thing)]
            :message "the list blah"
            :list [{:raw '(raw-e raw-a)
                    :result ["result"]
                    :expected-message "expected-message"
                    :actual-message "actual-message"
                    :message "message"}
                   {:raw '(raw-e2 raw-a2)
                    :result ["result2"]
                    :expected-message "expected-message2"
                    :actual-message "actual-message2"
                    :message "message2"}]})))

(expect "           result
\n           expected-message\n           actual-message\n           message"
        (with-redefs [show-raw-choice (constantly false)]
          (->failure-message
           {:raw '(raw-e raw-a)
            :result ["result"]
            :expected-message "expected-message"
            :actual-message "actual-message"
            :message "message"})))

(expect ""
        (with-redefs [show-raw-choice (constantly true)]
          (->failure-message {})))

(expect ""
        (with-redefs [show-raw-choice (constantly false)]
          (->failure-message {})))
