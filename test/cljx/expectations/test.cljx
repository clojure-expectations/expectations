(ns expectations.test
  (:require [success.success-examples]
            [expectations :as e]))

#+cljs
(enable-console-print!)
#+cljs
(set! *main-cli-fn* #(e/run-all-tests))
