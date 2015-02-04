(ns expectations.platform
  (:refer-clojure :exclude [all-ns format ns-interns])
  #+cljs (:require-macros [expectations.platform.cljs :as cljs])
  (:require #+clj [clojure.pprint :as pprint]
            #+cljs [goog.string]
            #+cljs [goog.string.format]))

(defn all-ns []
  #+clj (clojure.core/all-ns)
  #+cljs (cljs/all-ns*))

(defn ns-interns [ns]
  #+clj (clojure.core/ns-interns ns)
  #+cljs (cljs/ns-interns* ns))

(def format
  #+clj clojure.core/format
  #+cljs goog.string/format)

(def pprint
  #+clj pprint/pprint
  #+cljs println)                                           ;until there's a usable cljs pprint port
