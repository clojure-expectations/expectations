(ns expectations.test
  #?(:cljs (:require-macros [expectations.cljs :as ecljs]))
  (:require [success.expectations-options]
            [success.success-examples]
            [success.nested.success-examples]))

#?(:cljs
   (defn -main []
     (ecljs/run-all-tests)))

#?(:cljs (enable-console-print!))
#?(:cljs (set! *main-cli-fn* -main))
