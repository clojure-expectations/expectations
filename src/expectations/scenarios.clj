(ns expectations.scenarios
  (:require expectations)
  (:use [expectations :only [doexpect fail stack->file&line report]]))

(defn in [n] {:expectations/in n :expectations/in-flag true})

(defmacro expect
  ([e a] `(binding [fail (fn [name# msg#] (throw (expectations.junit.ScenarioError. name# msg#)))]
	    (doexpect ~e ~a)))
  ([a] `(binding [fail (fn [name# msg#] (throw (expectations.junit.ScenarioError name# msg#)))]
	  (doexpect :expectations/true ~a))))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym "test") assoc :expectation true)
	(fn []
	  (try
	    ~@forms
	    (catch AssertionError e#
	      (fail (.name e#) (str (.getMessage e#) "\n" (expectations/pruned-stack-trace e#))))
	    (catch Throwable t#
	      (report {:type :error :result t#}))))))