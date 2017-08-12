(ns readme (:require expectations))

(expectations/expect "This is text!"
	(str "This is " "text!"))



(expectations/expect 420
	(* 42 13))





(expectations/expect 6
	(+ 1
   2
   3))



(defn foo [a] (* a a))



(expectations/expect 9
	(foo 3))




