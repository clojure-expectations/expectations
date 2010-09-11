(ns scenario-examples
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