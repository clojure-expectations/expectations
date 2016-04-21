(ns expectations.platform
  (:refer-clojure :exclude [bound? format ns-name])
  (:require #?(:clj [clojure.pprint :as pprint])
            #?(:cljs [cljs.analyzer])
            #?(:cljs [goog.string])
            #?(:cljs [goog.string.format]))
  #?(:clj (:import (clojure.lang Agent Atom Ref))))

#?(:clj
   (defmacro cljs? []
     (boolean (:ns &env))))

#?(:clj
   (defn expanding [n]
     (if (cljs?)
       `(cljs.analyzer/macroexpand-1 {} '~n)
       `(macroexpand-1 '~n))))

#?(:clj
   (defn err-type []
     (if (cljs?) `~'js/Error `Throwable)))

(defn ns-name [ns]
  #?(:clj (if (symbol? ns) ns (clojure.core/ns-name ns))
     :cljs (if (symbol? ns) ns)))

(def bound?
  #?(:clj clojure.core/bound?
     :cljs (fn [& vars] (every? #(deref %) vars))))

(def format
  #?(:clj clojure.core/format
     :cljs goog.string/format))

(defn nodejs? []
  #?(:clj false
     :cljs (= (js* "typeof(process)") "object")))

(defn getenv [var]
  #?(:clj (System/getenv var)
     :cljs (aget (if (nodejs?) js/process.env js/window) var)))

(defn get-message [e] (-> e
                          #?(:clj .getMessage
                             :cljs .-message)))

(defn nano-time []
  #?(:clj (System/nanoTime)
     :cljs (if (nodejs?)
             (-> js/process .hrtime js->clj
                 (#(+ (* 1e9 (% 0)) (% 1))))
             (js/performance.now))))


(defn on-windows? []
  (re-find #"[Ww]in"
           #?(:clj (System/getProperty "os.name")
              :cljs (if (nodejs?) (.-platform js/process) ""))))

(def pprint
  #?(:clj pprint/pprint
     :cljs println))                                           ;until there's a usable cljs pprint port

(defn print-stack-trace [e]
  (-> e
      #?(:clj .printStackTrace
         :cljs .-stack) println))

(def iref-types
  #?(:clj #{Agent Atom Ref}
     :cljs #{cljs.core/Atom}))
