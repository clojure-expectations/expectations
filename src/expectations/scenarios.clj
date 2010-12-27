(ns expectations.scenarios
  (:require expectations)
  (:use [expectations :only [doexpect fail test-file stack->file&line report]]))

(defn in [n] {:expectations/in n :expectations/in-flag true})
(defmacro given [bindings form & args]
  (if args
    `(clojure.template/do-template ~bindings ~form ~@args)
    `(clojure.template/do-template [~'x ~'y]
                                   ~(list 'expect 'y (list 'x bindings)) ~@(rest form))))

(defmacro stubbing [bindings & forms]
  (let [new-bindings (reduce (fn [a [x y]] (conj a x `(fn [& _#] ~y))) [] (partition 2 bindings))]
    `(binding ~new-bindings ~@forms)))

(defmacro expect [e a]
  `(binding [fail (fn [test-file# test-meta# msg#] (throw (AssertionError. msg#)))]
    (doexpect ~e ~a)))

(defmacro doscenario [forms]
  `(try
    ~@forms
    (catch Throwable t#
      (report {:type :error :result t#}))))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym) assoc :expectation true)
    (fn [] (doscenario ~forms))))

(defmacro scenario-focused [& forms]
  `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
    (fn [] (doscenario ~forms))))
