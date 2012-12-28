(ns expectations-options
  (:require expectations))

(defn turn-iref-warnings-on
  "turn iref modification warnings on"
  {:expectations-options :before-run}
  []
  (expectations/warn-on-iref-updates))
