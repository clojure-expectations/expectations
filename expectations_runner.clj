(ns expectations-runner
  (:require expectations_test)
  (:require expectations))

(expectations/disable-run-on-shutdown)

(expectations/run-all-tests)


