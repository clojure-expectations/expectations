(ns expectations.expectations-expectations
  (:use expectations expectations))

(expect "\n
           -- got: (hello-fn {} 3 4)
           - arg1: {}
           :a with val 1 is in expected, but not in actual
           :b with val 2 is in expected, but not in actual
           - arg2: 3
           - arg3: 4"
        (compare-args "hello-fn" [{:a 1 :b 2} 2 3] [{} 3 4]))

(expect "\n
           -- got: (hello-fn 1 2 3 4)"
        (compare-args "hello-fn" [1 2] [1 2 3 4]))

(expect "\n
           -- got: (hello-fn 1 2)
           - arg3: nothing
           - arg4: nothing"
        (compare-args "hello-fn" [1 2 3 4] [1 2]))

(expect "\n
           -- got: (hello-fn {} 3 4)
           - arg1: {}
           :a with val 1 is in expected, but not in actual
           :b with val 2 is in expected, but not in actual
           - arg3: 4"
        (compare-args "hello-fn" [{:a 1 :b 2} anything 3] [{} 3 4]))

(expect "\n
           -- got: (hello-fn {} 3 4)
           - arg1: {}
           :a with val 1 is in expected, but not in actual
           :b with val 2 is in expected, but not in actual"
        (compare-args "hello-fn" [{:a 1 :b 2} anything&] [{} 3 4]))

(expect "\n
           -- got: (hello-fn 1 2)"
        (compare-args "hello-fn" nil [1 2]))

(expect "\n
           -- got: (hello-fn)
           - arg1: nothing
           - arg2: nothing"
        (compare-args "hello-fn" [1 2] nil))
