This is an example markup file used to test the generation of the readme tests.
```clojure
(str "This is " "text!")
=> "This is text!"
```
And another example:
``` clj
(* 42 13)
=> 420
```
Here is a
multi-line
example:
```clojure
(+ 1
   2
   3)
=> 6
```
Clojure code with no expectation -- should copy across:
```clojure
(defn foo [a] (* a a))
```
So we can test it:
```clj
(foo 3)
=> 9
```
This is not Clojure code:
```sh
lein expectations
```
