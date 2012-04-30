(ns failure.scenario_failure_examples
  (:import [org.joda.time DateTime])
  (:use expectations.scenarios))

(defn thr [] (/ 12 0))
(defn two [] (thr))
(defn one [] (two))

(scenario
  (let [a (atom 0)]
    (swap! a inc)
    (expect 1 @a)
    (swap! a inc)
    (expect 2 @a)
    (one)
    (expect 1 @a)
    (expect "1" "2")))

(scenario
  (let [a (atom 0)]
    (swap! a inc)
    (expect 1 @a)
    (swap! a inc)
    (expect 2 @a)
    (expect 1 (deref a))
    (expect "1" "2")))

(scenario
  (given [x y] (expect x y)
    1 88
    3 77))

;;; a passing one helps too
(scenario
  (stubbing [one 99]
    (expect 1 (one))))

(defn foo [] (println "hi"))
(defn bar [] (foo))

(defn foo2 [a b] (println a b))
(defn bar2 [a b] (foo2 (* a a) (* b b)))

(defn foo3 [& args] (println args))
(defn bar3 [a b] (foo3))

(def atom1 (atom 1))
(def ref1 (ref 1))


;; failure interaction tests
(scenario
  (expect (interaction (foo)) :once))

(scenario
  (expect (interaction (foo))))

(scenario
  (bar2 1 2)
  (expect (interaction (foo2 1 4)) :twice))

(scenario
  (bar2 1 2)
  (expect (interaction (foo2 1 4)) :never))

(scenario
  (bar2 3 2)
  (bar2 2 2)
  (expect (interaction (foo2 1 4)) :once))

(scenario
  :reminder "you should see this reminder"
  (expect (interaction (foo2 1 (/ 4 0))) :once))

(scenario
  (bar3 1 2)
  (expect (interaction (foo3 1 2)) :once))

(scenario
  (foo3 "1" 2)
  (expect (interaction (foo3 1 2)) :once))

(scenario
  (expect (interaction (foo 1 2)) :once))

(scenario
  (localize-state failure.scenario_failure_examples
    (swap! atom1 inc)
    (expect 1 @atom1)))

(scenario
  (localize-state failure.scenario_failure_examples
    (dosync (alter ref1 inc))
    (expect 1 @ref1)))

(scenario
  :reminder "you should see this reminder"
  (localize-state failure.scenario_failure_examples
    (swap! atom1 inc)
    (expect 2 @atom1))
  (expect 2 @atom1))

(scenario
  (localize-state failure.scenario_failure_examples
    (dosync (alter ref1 inc))
    (expect 2 @ref1))
  (expect 2 @ref1))

(scenario
  :freeze-time true
  (expect true
    (not=
      (DateTime.)
      (do (Thread/sleep 1) (DateTime.)))))
