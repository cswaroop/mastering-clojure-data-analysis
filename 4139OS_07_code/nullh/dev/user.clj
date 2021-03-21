(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:import
    [org.apache.poi.ss.usermodel Cell CellStyle DateUtil]
    [org.apache.poi.ss.usermodel Row Sheet])
  (:require
    [clojure.core.reducers :as r]
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [clojure.data.csv :as csv]
    [clojure.data.json :as json]
    [me.raynes.fs :as fs]
    [incanter.core :as i]
    [incanter.excel :as ixl]
    [incanter.charts :as c]
    [incanter.stats :as s]
    [incanter.mongodb :as im]
    [somnium.congomongo :as m]
    [nullh]
    [nullh.ucr :as ucr]
    [nullh.data :as d]
    [nullh.xl :as xl]
    [nullh.unodc :as un]
    [nullh.geo :as ngeo]
    [nullh.graphs :as g]
    [nullh.db :as db]
    [nullh.stats :as n-stat]
    [nullh.charts :as n-ch]
    [nullh.utils :as u]))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root
    #'system
    (fn [_] {})))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root
    #'system
    (fn [s]
      (let [cts (d/read-cts-data "unodc-data")
            lnd (d/read-lnd-data "ag.lnd")
            j (->> (d/join-all lnd cts)
                d/->maps
                d/pivot-data
                d/remove-missing
                i/to-dataset)
            agg (d/aggregate-by-country j)]
        (assoc s
               :cts cts
               :lnd lnd
               :j j
               :agg agg
               :lm (n-stat/stat-test agg 3 2))))))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  ;; TODO
  )

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))
