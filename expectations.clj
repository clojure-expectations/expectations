(ns expectations
  (:use clojure.test))

(.addShutdownHook
 (Runtime/getRuntime)
 (Thread. run-all-tests))

(defmethod report :begin-test-ns [m]
    (with-test-out
      (when (some #(:test (meta %)) (vals (ns-interns (:ns m))))
        (println "\nTesting" (ns-name (:ns m))))))

(defn which-comparison [expected actual options]
  (cond
   (and (:in options) (instance? java.util.Map expected)) :in-map
   (:in options) :in-set
   (instance? java.util.regex.Pattern expected) :regex
   (isa? expected Exception) :exception
   (= (class expected) Class) :class
   :default :equal))

(defmulti comparison which-comparison)

(defmethod comparison :equal [expected actual options] =)

(defmethod comparison :in-map [expected actual options]
	   (fn [e a] (= e (select-keys a (keys e)))))

(defmethod comparison :in-set [expected actual options]
	   (fn [e a] (a e)))

(defmethod comparison :regex [expected actual options] re-seq)

(defmethod comparison :exception [expected actual options] 'thrown?)

(defmethod comparison :class [expected actual options] instance?)

(def in {:in true})

(defmacro expect 
  ([expected actual]
     `(deftest ~(gensym)
	(is (~(comparison (eval expected) actual {}) ~expected ~actual))))
  ([expected option actual]
     `(deftest ~(gensym)
	(is (~(comparison (eval expected) actual (eval option)) ~expected ~actual)))))

(expect {:foo 1} in (assoc {:bar 1} :foo 1)) 

(expect :foo in #{:foo :bar}) 

(expect 2 (inc 1))

(expect "foo" "foo")

(expect #"foo" (str "boo" "foo" "ar"))

(expect ArithmeticException (/ 12 0))

(expect String "foo")