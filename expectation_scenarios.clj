(ns expectation-scenarios
  (:use expectations))

(defn thr [] (/ 12 0))
(defn two [] (thr))
(defn one [] (two))

(scenario
 (let [a (atom 0)]
   (swap! a inc)
   (check 1 @a)
   (swap! a inc)
   (check 2 @a)
   (one)
   (check 1 @a)
   (check "1" "2")))

(scenario
 (let [a (atom 0)]
   (swap! a inc)
   (check 1 @a)
   (swap! a inc)
   (check 2 @a)
;   (one)
   (check 1 @a)
   (check "1" "2")))