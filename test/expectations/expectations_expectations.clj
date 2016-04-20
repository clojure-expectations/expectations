(ns expectations.expectations-expectations
  (:use expectations))

(expect "           result

           expected-message
           actual-message
           message"
        (with-redefs [show-raw-choice (constantly false)]
          (->failure-message
           {:raw "raw"
            :result ["result"]
            :expected-message "expected-message"
            :actual-message "actual-message"
            :message "message"
            })))

(expect "[36m(expect raw-e raw-a)\n[0m
           result

           expected-message
           actual-message
           message"
        (with-redefs [show-raw-choice (constantly true)]
          (->failure-message
           {:raw '(raw-e raw-a)
            :result ["result"]
            :expected-message "expected-message"
            :actual-message "actual-message"
            :message "message"})))

(expect "[36m(expect (whole thing) nil)\n[0m
           the list blah

[36m(expect raw-e raw-a)\n[0m
           result

           expected-message
           actual-message
           message

[36m(expect raw-e2 raw-a2)\n[0m
           result2

           expected-message2
           actual-message2
           message2"

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
                    :message "message2"}]}))

)

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
