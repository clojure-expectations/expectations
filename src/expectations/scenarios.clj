(ns expectations.scenarios
  (:require expectations)
  (:use clojure.walk
        [expectations :only [doexpect fail test-file stack->file&line report]]))

(declare *interactions*)
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

(defmacro interaction [[f & args]]
  `(hash-map :expectations/interaction-flag true
             :function ~(str f)
             :interactions (@*interactions* ~(str f))
             :expected-args  (vector ~@args)))

(defn detect-interactions [v]
  (when (seq? v)
    (if (= "expect" (str (first v)))
      (let [expect-args (flatten (next v))]
        (if (= "interaction" (str (first expect-args)))
          (second expect-args)))
      v)))

(defn append-interaction [f-name]
  (fn [& args] (dosync (commute *interactions* update-in [f-name] conj args))))

(defmacro doscenario [forms]
  (let [fns (distinct (remove nil? (flatten (prewalk detect-interactions forms))))
        binds (reduce (fn [a f] (conj a f `(append-interaction ~(str f)))) [] fns)]
    `(try
       (binding [*interactions* (ref {})]
         (binding ~binds
           ~@forms))
       (catch Throwable t#
         (report {:type :error :result t#})))))

(defmacro scenario [& forms]
  `(def ~(vary-meta (gensym) assoc :expectation true)
     (fn [] (doscenario ~forms))))

(defmacro scenario-focused [& forms]
  `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
     (fn [] (doscenario ~forms))))
