(ns nullh.charts
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            [nullh.utils :as u]))

(defn plot-crime [dset indicator-label crime-label]
  (let [x (i/sel dset :cols :indicator-value)
        y (i/sel dset :cols :rate)
        title (str indicator-label " and " crime-label)]
    (c/scatter-plot x y
                    :title title
                    :x-label indicator-label
                    :y-label crime-label)))

(defn plot-agg
  ([agg-dset] (plot-agg agg-dset "Indicator"))
  ([agg-dset indicator-label] (plot-agg agg-dset indicator-label 3 2))
  ([agg-dset indicator-label x-col y-col]
   (let [x (i/sel agg-dset :cols x-col)
         y (i/sel agg-dset :cols y-col)]
     (c/scatter-plot x y
                     :x-label "Crime Rate"
                     :y-label indicator-label))))

(defn plot-agg-lm
  ([agg-dset lm] (plot-agg-lm agg-dset lm "Indicator"))
  ([agg-dset lm indicator-label] (plot-agg-lm agg-dset lm indicator-label 3 2))
  ([agg-dset lm indicator-label x-col y-col]
   (let [x (i/sel agg-dset :cols x-col)
         chart (plot-agg agg-dset indicator-label x-col y-col)]
     (c/add-lines chart x (:fitted lm)))))

(comment

(reset)
(pst)

(def by-ag-lnd
  (d/load-join-pivot
    "unodc-data"
    "ag.lnd/ag.lnd.totl.k2_Indicator_en_csv_v2.csv"))
(def ag-plot
  (n-ch/plot-crime (d/by-crime by-ag-lnd "CTS 2012 Burglary")
                   "Land Area" "Burglary"))
(def burglary
  (-> by-ag-lnd
    (d/by-crime "CTS 2012 Burglary")
    (d/aggregate-years-by-country :mean)))
(def ag-plot
  (n-ch/plot-crime burglary "Land Area" "Burglary"))
(i/view ag-plot)

(def by-ny-gnp
  (d/load-join-pivot
    "unodc-data"
    "ny.gnp/ny.gnp.pcap.cd_Indicator_en_csv_v2.csv"))
(def ny-plot
  (n-ch/plot-crime (d/by-crime by-ny-gnp "CTS 2012 Burglary") "GNI/capita" "CTS 2012 Burglary"))
(i/view ny-plot)

(def ny-plot
  (-> by-ny-gnp
    (d/by-crime "CTS 2012 Burglary")
    (d/aggregate-years-by-country :mean)
    (n-ch/plot-crime "GNI/capita" "CTS 2012 Burglary")))

  )
