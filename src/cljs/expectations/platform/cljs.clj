(ns expectations.platform.cljs
  (:require [cljs.analyzer.api]))

(defmacro all-ns* []
  `'~(cljs.analyzer.api/all-ns))

(defmacro ns-interns* [ns]
  `'~(cljs.analyzer.api/ns-interns ns))
