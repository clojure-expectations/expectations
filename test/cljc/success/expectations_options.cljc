(ns success.expectations-options
  (:require [expectations]
            [success.success-examples-src]))

(defn turn-iref-warnings-on
  "turn iref modification warnings on"
  {:expectations-options :before-run}
  []
  (expectations/warn-on-iref-updates))

(defn am-i-done?
  "turn iref modification warnings on"
  {:expectations-options :after-run}
  []
  (println "yeah, you're done."))

(defn in-context
  "rebind a var to verify that the expecations are run in the defined context"
  {:expectations-options :in-context}
  [work]
  (with-redefs [success.success-examples-src/a-fn-to-be-rebound (constantly :a-rebound-val)]
    (work)))
