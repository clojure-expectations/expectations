(ns expectations.scenarios
  (:require expectations)
  (:use [expectations :only [doexpect fail stack->file&line report]]))

(defn in [n] {:expectations/in n :expectations/in-flag true})

(defmacro expect
  ([e a] `(binding [fail (fn [_# msg#] (throw (AssertionError. msg#)))]
	    (doexpect ~e ~a)))
  ([a] `(binding [fail (fn [_# msg#] (throw (AssertionError. msg#)))]
	  (doexpect :expectations/true ~a))))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym "test") assoc :expectation true)
	(fn []
	  (try
	    ~@forms
	    (catch AssertionError e#
	      (fail (stack->file&line e# 5) (.getMessage e#)))
	    (catch Throwable t#
	      (report {:type :error :result t#}))))))