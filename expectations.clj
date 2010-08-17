(ns expectations
  (:use clojure.test))

(-> (Runtime/getRuntime) (.addShutdownHook (Thread. run-all-tests)))

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

(defmethod comparison :in-set [expected actual options] (fn [e a] (a e)))

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