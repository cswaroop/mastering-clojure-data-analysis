
(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [incanter.core :as i]
            [incanter.charts :as c]
            [me.raynes.fs :as fs]
            [clj-gis.download :as dl]
            [clj-gis.filter-data :as fd]
            [clj-gis.heatmap :as hm]
            [clj-gis.idw :as idw]
            [clj-gis.locations :as loc]
            [clj-gis.rolling-avg :as rolling]
            [clj-gis.system :as system]
            [clj-gis.types :as types]
            [clj-gis.util :as u]))

(def system nil)

(defn init
  []
  (alter-var-root #'system (constantly (system/system))))

(defn start
  []
  (alter-var-root #'system system/start))

(defn stop
  []
  (alter-var-root #'system (fn [s] (when s (system/stop s)))))

(defn go
  []
  (init)
  (start)
  nil)

(defn reset
  []
  (stop)
  (refresh :after 'user/go))

(defn update [f]
  (alter-var-root #'system f)
  nil)

(def data (mapv (fn [_] (types/->DataPoint (* (rand) 10) (* (rand) 10) (* (rand) 10))) (range 20)))

