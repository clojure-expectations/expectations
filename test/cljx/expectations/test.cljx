(ns expectations.test
  #+cljs (:require-macros [expectations.cljs :as ecljs])
  #+cljs (:require
           [success.success-examples]
           [success.nested.success-examples]))

#+cljs
(enable-console-print!)
#+cljs
(set! *main-cli-fn* #(ecljs/run-all-tests))
