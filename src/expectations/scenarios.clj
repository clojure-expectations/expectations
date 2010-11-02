(ns expectations.scenarios
  (:require expectations)
  (:use [expectations :only [doexpect fail stack->file&line report]]))

(defn in [n] {:expectations/in n :expectations/in-flag true})
(defmacro given [bindings form & args]
  (if args
    `(clojure.template/do-template ~bindings ~form ~@args)
    `(clojure.template/do-template [~'x ~'y] ~(list 'expect 'y (list 'x bindings)) ~@(rest form))))


(defmacro expect [e a]
  `(binding [fail (fn [name# v# msg#] (throw (expectations.junit.ScenarioError. name# v# msg#)))]
     (doexpect ~e ~a)))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym "test") assoc :expectation true)
	(fn []
	  (try
	    ~@forms
	    (catch AssertionError e#
	      (fail (.name e#) (.uniqueId e#) (str (.getMessage e#) "\n" (expectations/pruned-stack-trace e#))))
	    (catch Throwable t#
	      (report {:type :error :result t#}))))))