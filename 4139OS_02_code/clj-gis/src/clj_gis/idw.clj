
(ns clj-gis.idw
  (:require [clj-gis.types :refer :all]))

(defn set-min-max
  ([] {:min-lat Double/MAX_VALUE, :max-lat Double/MIN_VALUE,
       :min-lon Double/MAX_VALUE, :max-lon Double/MIN_VALUE})
  ([min-max d-temp]
   (let [lat (:lat d-temp)
         lon (:lon d-temp)
         {:keys [min-lat min-lon max-lat max-lon]} min-max]
     {:min-lat (min min-lat lat), :max-lat (max max-lat lat)
      :min-lon (min min-lon lon), :max-lon (max max-lon lon)})))

(defn min-max-lat-lon
  [d-temps]
  (reduce set-min-max (set-min-max) d-temps))

(defn w
  ([p dist-fn x] (partial w p dist-fn x))
  ([p dist-fn x x_i]
   (/ 1.0 (Math/pow (dist-fn x x_i) p))))

(defn sum-over [f coll] (reduce + (map f coll)))
(defn idw
  ([sample-points data-key p dist-fn]
   (partial idw sample-points data-key p dist-fn))
  ([sample-points data-key p dist-fn point]
   (float
     (/ (sum-over #(* (w p dist-fn point %) (data-key %))
                  sample-points)
        (sum-over (w p dist-fn point) sample-points))))
  ([sample-points data-key p dist-fn lat lon]
   (idw sample-points data-key p dist-fn
        (->DataPoint lat lon nil))))

