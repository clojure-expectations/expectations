(ns expectations.platform.cljs
  (:require [cljs.analyzer.api]))

(defmacro all-ns* []
  `'~(cljs.analyzer.api/all-ns))

(defmacro ns-vars* []
  (into {} (for [ns (cljs.analyzer.api/all-ns)]
             [`'~ns (->> (cljs.analyzer.api/ns-interns ns)
                      keys
                      (map (fn [k] `(var ~(symbol (name ns) (name k)))))
                      (into []))])))
