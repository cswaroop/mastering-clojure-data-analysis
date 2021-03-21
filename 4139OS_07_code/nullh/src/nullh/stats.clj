(ns nullh.stats
  (:require [incanter.core :as i]
            [incanter.stats :as s]))

(defn stat-test
  ([agg-dset] (stat-test agg-dset 3 2))
  ([agg-dset x-col y-col]
   (let [x (i/sel agg-dset :cols x-col)
         y (i/sel agg-dset :cols y-col)]
     (s/linear-model y x))))

(defn spearmans
  ([col-a col-b] (spearmans col-a col-b i/$data))
  ([col-a col-b dataset]
   (let [rho (s/spearmans-rho
               (i/sel dataset :cols col-a)
               (i/sel dataset :cols col-b))
         n (i/nrow dataset)
         mu 0.0
         sigma (Math/sqrt (/ 1.0 (- n 1.0)))
         z (/ (- rho mu) sigma)]
     {:rho rho, :n n, :mu mu, :sigma sigma, :z z})))

