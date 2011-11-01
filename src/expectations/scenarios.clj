(ns expectations.scenarios
  (:require expectations)
  (:use clojure.walk
        [expectations :only [doexpect fail test-file stack->file&line report]]))

(def ^:dynamic *interactions*)
(def in expectations/in)

(defmacro given [bindings form & args]
  (let [s (gensym "local")]
    (if args
      `(clojure.template/do-template ~bindings ~form ~@args)
      `(let [~s ~bindings]
         (clojure.template/do-template [~'f ~'expected]
           ~(list 'expect 'expected (list 'f s)) ~@(rest form))))))

(defmacro stubbing [bindings & forms]
  (let [new-bindings (reduce (fn [a [x y]] (conj a x `(fn [& _#] ~y))) [] (partition 2 bindings))]
    `(with-redefs ~new-bindings ~@forms)))

(defmacro expect [& args]
  (condp = (count args)
    1 `(binding [fail (fn [test-file# test-meta# msg#] (throw (AssertionError. msg#)))]
         (doexpect ~(first args) :once ))
    `(binding [fail (fn [test-file# test-meta# msg#] (throw (AssertionError. msg#)))]
       (doexpect ~@args))))

(defmacro interaction [[f & args]]
  `(hash-map :expectations/interaction-flag true
     :function ~(str f)
     :interactions (@*interactions* ~(str f))
     :expected-args (vector ~@args)))

(defn detect-interactions [v]
  (when (seq? v)
    (if (= "expect" (str (first v)))
      (let [expect-args (flatten (next v))]
        (if (= "interaction" (str (first expect-args)))
          (second expect-args)))
      v)))

(defn append-interaction [f-name]
  (fn [& args] (dosync (commute *interactions* update-in [f-name] conj args))
    (str f-name " result")))

(defn no-op [& _])

(def placeholder-fn)

(def anything :anything )

(defmulti localize class)
(defmethod localize clojure.lang.Atom [a] (atom @a))
(defmethod localize clojure.lang.Ref [a] (ref @a))
(defmethod localize :default [v] v)

(defn bind-to-localized [[var-name var]]
  (when (bound? var)
    (when-let [vv (var-get var)]
      (when (#{clojure.lang.Atom clojure.lang.Ref} (class vv))
        [var-name (list 'localize var-name)]))))

(defn default-local-vals [ns]
  (if (nil? ns)
    []
    (->> (ns-interns ns)
      (map bind-to-localized)
      (reduce into []))))

(defmacro localize-state [ns & forms]
  `(with-redefs ~(default-local-vals ns) ~@forms))

(defmacro doscenario [forms & {declarative-binds :binding
                               declarative-stubs :stubbing
                               declarative-localize-state :localize-state
                               reminder :reminder}]
  (let [fns (distinct (remove nil? (flatten (prewalk detect-interactions forms))))
        binds (reduce (fn [a f] (conj a f `(append-interaction ~(str f)))) [] fns)]
    `(try
       (localize-state ~declarative-localize-state
         (stubbing ~(vec declarative-stubs)
           (binding [expectations/reminder ~reminder]
             (with-redefs ~(vec declarative-binds)
               (binding [*interactions* (ref {})]
                 (with-redefs ~binds
                   ~@forms))))))
       (catch Throwable t#
         (report {:type :error :result t#})))))

(defmacro scenario [& forms]
  (let [[decs fs] ((juxt take-while drop-while) #(not= (class %) clojure.lang.PersistentList) forms)]
    `(def ~(vary-meta (gensym) assoc :expectation true)
       (fn [] (doscenario ~fs ~@decs)))))

(defmacro scenario-focused [& forms]
  (let [[decs fs] ((juxt take-while drop-while) #(not= (class %) clojure.lang.PersistentList) forms)]
    `(def ~(vary-meta (gensym) assoc :expectation true :focused true)
       (fn [] (doscenario ~fs ~@decs)))))
