
(ns sentiment.err
  (:require [sentiment.util :as util]))

(defn sum-squares [coll]
  (reduce + 0 (map #(Math/pow % 2) coll)))

(defn sse [actuals estimates]
  "Sum squared error."
  (sum-squares (map - actuals estimates)))

(defn ssr [actuals-seq estimates]
  "Sum of squared residuals."
  (sum-squares (map - estimates (map util/mean actuals-seq))))

(defn sst [actuals estimates]
  "Total sum of squares."
  (+ (ssr actuals estimates) (sse actuals estimates)))

(defn r-square [actuals estimates]
  (let [ssr' (ssr actuals estimates)
        sse' (sse actuals estimates)
        sst' (+ ssr' sse')]
    (/ ssr' sst')))

