
(ns sentiment.system
  (:require [sentiment.data :as d]
            [sentiment.tokens :as t]))

(defn system []
  {:data nil})

(defn start [system]
  (let [reviews (->> "data/hotels-sample"
                  d/read-data
                  d/sample
                  vals
                  flatten)
        [f-index reviews] (d/add-features t/unigrams reviews)]
    (assoc system
           :reviews reviews
           :f-index f-index
           :data reviews)))

(defn stop [system]
  system)

