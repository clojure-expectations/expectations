(ns expectations.platform.macros
  (:require [cljs.analyzer.api]))

(defn cljs? []
  (boolean (find-ns 'cljs.core)))

(defmacro all-ns* []
  (if (cljs?)
    `'~(cljs.analyzer.api/all-ns)
    `'~(map ns-name (all-ns))))

(defmacro ns-vars* []
  (if (cljs?)
    (into {} (for [ns (cljs.analyzer.api/all-ns)]
               [`'~ns (->> (cljs.analyzer.api/ns-interns ns)
                        keys
                        (map (fn [k] `(var ~(symbol (name ns) (name k)))))
                        (into []))]))
    `(into {} (for [ns# (all-ns)]
                [(ns-name ns#) (into [] (vals (ns-interns ns#)))]))))
