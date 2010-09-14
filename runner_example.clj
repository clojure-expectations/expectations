(ns runner-example
  (:require 
    examples.success-examples
    examples.failure-examples
    examples.scenario-examples
    expectations))

(expectations/disable-run-on-shutdown)

(expectations/run-all-tests)


