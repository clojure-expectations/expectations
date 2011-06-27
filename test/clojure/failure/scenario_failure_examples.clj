(ns failure.scenario_failure_examples
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

;; failure interaction tests
(scenario
 (expect (interaction (foo)) :once))

(scenario
 (expect (interaction (foo))))

(scenario
 (expect (interaction (foo)) 4 4))

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
 (expect (interaction (foo2 1 (/ 4 0))) :once))

(scenario-focused
 (bar3 1 2)
  (println "hi")
 (expect (interaction (foo3 1 2)) :once))

(scenario
 (expect (interaction (foo 1 2)) :once))
