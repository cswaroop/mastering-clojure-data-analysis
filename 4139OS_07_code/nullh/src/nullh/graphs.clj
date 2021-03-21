
(ns nullh.graphs
  (require [clojure.string :as str]
           [incanter.core :as i]
           [incanter.charts :as c]
           [incanter.stats :as s]))


(defn make-label [kw]
  (let [words (-> kw
                name
                (str/replace \_ \space)
                (str/replace \- \space)
                (str/split #"\s+"))]
    (->> words
      (map str/capitalize)
      (str/join \space))))

;;;; Some interesting ones to run: :year, :state
(defn col-frequency [dataset col]
  (let [freqs (sort-by first (frequencies (i/sel dataset :cols col)))
        label (make-label col)]
    (c/bar-chart (map first freqs) (map second freqs)
                 :y-label "Frequency" :x-label label
                 :title (str label " Frequencies"))))

;;;; Some interesting ones to run: :population, :*_rate
(defn col-histogram [dataset col]
  (let [label (make-label col)]
    (c/histogram (i/sel dataset :cols col)
                 :nbins 20
                 :title (str label " Histogram")
                 :x-label label)))

(defn dot-by [dataset x-col y-col]
  (let [x-label (make-label x-col)
        y-label (make-label y-col)]
    (c/scatter-plot (i/sel dataset :cols x-col)
                    (i/sel dataset :cols y-col)
                    :title (str y-label " by " x-label)
                    :x-label x-label
                    :y-label y-label)))

(defn dot-by-linear [dataset x-col y-col]
  (let [x-label (make-label x-col)
        y-label (make-label y-col)
        xs (i/sel dataset :cols x-col)
        ys (i/sel dataset :cols y-col)
        lm (s/linear-model ys xs)
        chart (c/scatter-plot xs ys
                              :title (str y-label " by " x-label)
                              :x-label x-label
                              :y-label y-label)]
    (c/add-lines chart xs (:fitted lm))
    {:chart chart
     :linear-model lm}))

