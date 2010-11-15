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
    (expect 1 @a)
    (expect "1" "2")))

(scenario
  (given [x y] (expect x y)
    1 2
    3 4))

; a passing one helps too
(scenario
  (given [x y] (expect x y)
    1 1
    3 3))