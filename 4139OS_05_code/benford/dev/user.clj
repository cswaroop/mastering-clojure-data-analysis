
(ns user
  (:require [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.set :as set]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.io :as iio]
            [benford.core :as b]
            [benford.system :as system]))

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

(defn update-data [coll]
  (update (fn [s]
            (let [freqs (b/first-digit-freq coll)]
              (assoc s
                     :data coll
                     :freqs freqs
                     :ratios (b/freq->ratio freqs))))))

(defn benford-test []
  (b/x-sqr (map b/benford (range 1 10)) (map (:freqs system) (range 1 10))))

