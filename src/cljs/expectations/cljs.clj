(ns expectations.cljs
  (:require [cljs.analyzer.api :as aapi]))

(defmacro run-tests [nss]
  `(println ~nss))                                          ;TODO impl

(defmacro run-all-tests
  ([] `(run-all-tests nil))
  ([re] `(run-tests
           [~@(->> (cond->> (aapi/all-ns)
                     re (filter #(re-matches re (name %))))
                (map (fn [s] `'~s)))])))