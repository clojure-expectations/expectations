(ns expectations.platform
  (:refer-clojure :exclude [all-ns bound? format ns-interns])
  #+cljs (:require-macros [expectations.platform.cljs :as cljs])
  (:require #+clj [clojure.pprint :as pprint]
            #+cljs [goog.string]
            #+cljs [goog.string.format]))

(defn all-ns []
  #+clj (clojure.core/all-ns)
  #+cljs (cljs/all-ns*))

(def bound?
  #+clj clojure.core/bound?
  #+cljs (fn [& vars] (every? #(deref %) vars)))

(def format
  #+clj clojure.core/format
  #+cljs goog.string/format)

(defn ns-interns [ns]
  #+clj (clojure.core/ns-interns ns)
  #+cljs (cljs/ns-interns* ns))

(def pprint
  #+clj pprint/pprint
  #+cljs println)                                           ;until there's a usable cljs pprint port
