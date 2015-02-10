(ns expectations.platform
  (:refer-clojure :exclude [all-ns bound? format ns-name])
  #+cljs (:require-macros [expectations.platform.cljs :as cljs])
  (:require #+clj [clojure.pprint :as pprint]
            #+clj [cljs.analyzer]
            #+cljs [goog.string]
            #+cljs [goog.string.format])
  #+clj (:import (clojure.lang Agent Atom Ref)))

(defn cljs? []
  #+clj (boolean (find-ns 'cljs.core))
  #+cljs true)

(defn all-ns []
  #+clj (clojure.core/all-ns)
  #+cljs (cljs/all-ns*))

(defn ns-name [ns]
  #+clj (clojure.core/ns-name ns)
  #+cljs (if (symbol? ns) ns))

(defn ns-vars []
  #+clj (->> (all-ns)
          (map (fn [ns] [(ns-name ns) ((comp (partial into []) vals ns-interns) ns)]))
          (into {}))
  #+cljs (cljs/ns-vars*))

(def bound?
  #+clj clojure.core/bound?
  #+cljs (fn [& vars] (every? #(deref %) vars)))

(def format
  #+clj clojure.core/format
  #+cljs goog.string/format)

(defn getenv [var]
  #+clj (System/getenv var)
  #+cljs (aget (.-env js/process) var))

(defn get-message [e] (-> e
                        #+clj .getMessage
                        #+cljs .-message))

(defn nano-time []
  #+clj (System/nanoTime)
  #+cljs (-> js/process .hrtime js->clj
           (#(+ (* 1e9 (% 0)) (% 1)))))

(defn on-windows? []
  (re-find #"[Ww]in"
    #+clj (System/getProperty "os.name")
    #+cljs (.-platform js/process)))

(def pprint
  #+clj pprint/pprint
  #+cljs println)                                           ;until there's a usable cljs pprint port

(defn print-stack-trace [e]
  (-> e
    #+clj .printStackTrace
    #+cljs .-stack println))

(def reference-types
  #+clj #{Agent Atom Ref}
  #+cljs #{cljs.core/Atom})
