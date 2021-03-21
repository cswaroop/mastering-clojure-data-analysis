
(ns clj-gis.heatmap
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            [clj-gis.idw :as idw]
            [clj-gis.types :refer :all]))

(defn rand-in
  [rng]
  (let [[from to] rng]
    (+ (rand (- to from)) from)))

(defn rand-point
  [x-range y-range data-range]
  (->DataPoint (rand-in x-range)
               (rand-in y-range)
               (rand-in data-range)))

(defn rand-points
  [x-range y-range data-range n]
  (doall
    (map (fn [_] (rand-point x-range y-range data-range))
         (range n))))

(defn euclidean-dist
  "For testing."
  [a b]
  (let [{x1 :lon, y1 :lat} a
        {x2 :lon, y2 :lat} b]
    (Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))

(defn generate-hm
  [sample p border]
  (let [{:keys [min-lat max-lat min-lon max-lon]} (idw/min-max-lat-lon sample)]
    (->
      (c/heat-map (idw/idw sample :value p euclidean-dist)
                  (- min-lon border) (+ max-lon border)
                  (- min-lat border) (+ max-lat border))
      (c/set-alpha 0.5)
      (c/add-points (map :lon sample) (map :lat sample)))))

(defn generate-idw
  ([] (generate-idw [0.0 100.0] [0.0 100.0] [0.0 10.0] 30 4.0))
  ([p] (generate-idw [0.0 100.0] [0.0 100.0] [0.0 10.0] 30 p))
  ([n p] (generate-idw [0.0 100.0] [0.0 100.0] [0.0 10.0] n p))
  ([x-range y-range data-range n p]
   (let [sample (rand-points x-range y-range data-range n)]
     [sample (generate-hm sample p 5.0)])))

