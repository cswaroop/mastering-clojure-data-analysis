
(ns user
  (:require [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.set :as set]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [sentiment.analysis :as sa]
            [sentiment.compare :as c]
            [sentiment.data :as d]
            [sentiment.system :as system]
            [sentiment.tokens :as t]
            [sentiment.util :as util]
            [sentiment.weka :as w]
            [sentiment.x-valid :as xv]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly (system/system))))

(defn start []
  (alter-var-root #'system system/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (system/stop s)))))

(defn go []
  (init)
  (start)
  nil)

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn update [f]
  (alter-var-root #'system f)
  nil)

