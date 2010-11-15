(ns expectations.scenarios
  (:require expectations)
  (:use [expectations :only [doexpect fail stack->file&line report]]))

(defn in [n] {:expectations/in n :expectations/in-flag true})
(defmacro given [bindings form & args]
  (if args
    `(clojure.template/do-template ~bindings ~form ~@args)
    `(clojure.template/do-template [~'x ~'y] ~(list 'expect 'y (list 'x bindings)) ~@(rest form))))

(defmacro expect [e a]
  `(binding [fail (fn [name# v# msg#] (throw (vary-meta (AssertionError. msg#) assoc :test-name name# :test-id v#)))]
     (doexpect ~e ~a)))

(defmacro doscenario [forms]
  `(try
     ~@forms
     (catch AssertionError e#
       (let [{name# :test-name id# :test-id} (meta e#)]
	 (fail name# id# (str (.getMessage e#) "\n" (expectations/pruned-stack-trace e#)))))
     (catch Throwable t#
       (report {:type :error :result t#}))))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym) assoc :expectation true)
	(fn [] (doscenario ~forms))))

(defmacro scenario-focused [& forms]
  `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
	(fn [] (doscenario ~forms))))
