(ns expectations.test
  (:require #+cljs [cljs.nodejs :as node]
            [success.success-examples]))

#+cljs
(enable-console-print!)

#+cljs
(set! *main-cli-fn* (fn []))
