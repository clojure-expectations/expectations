(ns runner-example
  (:require 
    success-examples 
    failure-examples
    scenario-examples
    expectations))

(expectations/disable-run-on-shutdown)

(expectations/run-all-tests)


