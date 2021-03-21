
(ns benford.system
  (:require [benford.core :as b]))

;; The compound interest data
(def data-coll [ 100.00
                 110.47
                 122.04
                 134.82
                 148.94
                 164.53
                 181.76
                 200.79
                 221.82
                 245.04
                 270.70
                 299.05
                 330.36
                 364.96
                 403.17
                 445.39
                 492.03
                 543.55
                 600.47
                 663.35
                 732.81
                 809.54
                 894.31
                 987.96
                1091.41])

(def fit-data [10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10
               10 10 10 10 10 10 10 10 10 20 20 20 20 20 20 20 20 20 20 20 20
               20 20 20 20 20 20 30 30 30 30 30 30 30 30 30 30 30 30 30 40 40
               40 40 40 40 40 40 40 40 50 50 50 50 50 50 50 50 60 60 60 60 60
               60 60 70 70 70 70 70 70 80 80 80 80 80 90 90 90 90]) 

(defn system []
  (let [freqs (b/first-digit-freq data-coll)]
    {:data data-coll
     :freqs freqs
     :ratios (b/freq->ratio freqs)}))

(defn start [system]
  (let [data (b/read-databank "data/population.csv")
        freqs (b/first-digit-freq data)]
    (assoc system
           :data data
           :freqs freqs
           :ratios (b/freq->ratio freqs))))

(defn stop [system]
  system)

