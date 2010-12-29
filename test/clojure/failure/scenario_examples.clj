(ns failure.scenario-examples
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
        1 2
        3 4))

;;; a passing one helps too
(scenario
 (given [x y] (expect x y)
        1 1
        3 3))

(scenario
 (stubbing [one 1
            two 2
            thr nil]
           (expect 1 (one))
           (expect 2 (two))
           (expect nil? (thr))))

(defn foo [] (println "hi"))
(defn bar [] (foo))

(defn foo2 [a b] (println a b))
(defn bar2 [a b] (foo2 (* a a) (* b b)))

(defn foo3 [& args] (println args))
(defn bar3 [a b] (foo3))

;; success interaction tests
(scenario-focused
 (bar)
 (expect (interaction (foo)) :once))

(scenario-focused
 (bar2 1 2)
 (expect (interaction (foo2 1 4)) :once))

(scenario-focused
 (bar2 1 2)
 (bar2 2 2)
 (expect (interaction (foo2 1 4)) :once))

;; failure interaction tests
(scenario-focused
 (expect (interaction (foo)) :once))

(scenario-focused
 (bar2 3 2)
 (bar2 2 2)
 (expect (interaction (foo2 1 4)) :once))

(scenario-focused
 (expect (interaction (foo2 1 (/ 4 0))) :once))

(scenario-focused
 (bar3 1 2)
 (expect (interaction (foo3 1 2)) :once))
