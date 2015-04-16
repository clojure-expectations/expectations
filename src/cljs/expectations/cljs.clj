(ns expectations.cljs
  (:require [cljs.analyzer.api :as aapi]
            [expectations :as e]))

(defn ->cljs-vars [namespace]
  (assert (symbol? namespace) (str namespace))
  (assert (aapi/find-ns namespace) (str namespace))
  (->> (aapi/ns-interns namespace)
    (filter (fn [[_ v]] (not (:macro v))))
    (map (fn [[k _]] `(var ~(symbol (name namespace) (name k)))))
    (into [])))

(defmacro run-tests [& namespaces]
  (assert (every? symbol? namespaces) (str namespaces))
  (assert (every? aapi/find-ns namespaces) (str "Some namespaces were not properly require'd: " namespaces))
  `(let [all-vars# [~@(mapcat ->cljs-vars namespaces)]
         all-vars# (sort-by (fn [v#] [(-> v# meta :ns) (-> v# meta :line)]) all-vars#)
         vars-by-kind# (e/by-kind all-vars#)
         expectations# (:expectation vars-by-kind#)]
     (if-let [focused# (:focused vars-by-kind#)]
       (doto (assoc (e/test-vars (assoc vars-by-kind# :expectation focused#) (- (count expectations#) (count focused#)))
               :type :summary)
         (e/report))
       (doto (assoc (e/test-vars vars-by-kind# 0)
               :type :summary)
         (e/report)))))

(defmacro run-all-tests
  ([] `(run-all-tests nil))
  ([re] `(run-tests
           ~@(cond->> (->> (aapi/all-ns)
                        (filter (comp not #(re-matches #"cljs\..+|clojure\..+|expectations(?:\.platform)?" %) name))
                        (filter aapi/find-ns))
               re (filter #(re-matches re (name %)))))))
