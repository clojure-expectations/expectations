(ns expectations.test
  (:require #+cljs [cljs.nodejs :as node]
            [expectations :as e]
            [success.success-examples]))

#+cljs
(enable-console-print!)

#+cljs
(set! *main-cli-fn* (fn [] (e/run-all-tests)))
