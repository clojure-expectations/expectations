(ns expectations.cljs
  (:require [cljs.analyzer.api :as aapi]))

(defmacro run-tests [nss]
  `(do
     (println ~nss)
     (println (count ~nss))))                                          ;TODO impl

(defmacro run-all-tests
  ([] `(run-all-tests nil))
  ([re] `(run-tests
           '~(cond->> (aapi/all-ns)
               re (filter #(re-matches re (name %)))))))