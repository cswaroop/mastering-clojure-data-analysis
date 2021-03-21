(ns nullh.data
  (:require [incanter.core :as i]
            [incanter.io :as iio]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [nullh.unodc :as unodc]
            [nullh.utils :as u]))

(defn read-cts-data [dirname]
  (let [input (mapcat unodc/read-sheets (u/ls dirname))
        cols (keys (first input))]
    (i/dataset cols (doall (map #(map second %) input)))))

(def headers [:country-name :country-code :indicator-name
              :indicator-code :1961 :1962 :1963 :1964 :1965 :1966
              :1967 :1968 :1969 :1970 :1971 :1972 :1973 :1974
              :1975 :1976 :1977 :1978 :1979 :1980 :1981 :1982
              :1983 :1984 :1985 :1986 :1987 :1988 :1989 :1990
              :1991 :1992 :1993 :1994 :1995 :1996 :1997 :1998
              :1999 :2000 :2001 :2002 :2003 :2004 :2005 :2006
              :2007 :2008 :2009 :2010 :2011 :2012 :2013])

(defn ->double [x] (if (str/blank? x) nil (Double/parseDouble x)))

(defn coerce-row [row]
  (let [[x y] (split-at 4 row)]
    (concat x (map ->double y))))

(defn read-indicator-data [filename]
  (with-open [f (io/reader filename)]
    (->> f
      csv/read-csv
      (drop 3)
      (map coerce-row)
      doall
      (i/dataset headers))))

(defn read-lnd-data [dirname]
  (read-indicator-data
    (str dirname \/ "ag.lnd.totl.k2_Indicator_en_csv_v2.csv")))

(defn read-gnp-data [dirname]
  (read-indicator-data
    (str dirname \/ "ny.gnp.pcap.cd_Indicator_en_csv_v2.csv")))

(defn join-all [indicator cts]
  (i/$join [:country-name :country] indicator cts))

(defn ->maps [dset]
  (let [col-names (i/col-names dset)]
    (map #(zipmap col-names %) (i/to-list dset))))

(defn pivot-year [m year]
  (let [count-key (keyword (str "count-" year))
        rate-key (keyword (str "rate-" year))
        year-key (keyword (str year))]
    (-> m
      (select-keys [:region :sub-region :country :country-code
                    :indicator :indicator-code
                    :sheet count-key rate-key year-key])
      (set/rename-keys {:sheet :crime,
                        count-key :count,
                        rate-key :rate,
                        year-key :indicator-value})
      (assoc :year year))))

(defn pivot-map [m]
  (let [years [2003 2004 2005 2006 2007 2008 2009 2010 2011]]
    (map #(pivot-year m %) years)))

(defn pivot-data
  "This takes a joined dataset of maps and outputs a new data
  with with these fields:

  * region
  * sub-region
  * country
  * country-code
  * indicator
  * indicator-code
  * crime
  * year
  * count
  * rate
  * indicator-value
  "
  [map-seq]
  (mapcat pivot-map map-seq))

(defn remove-missing
  "This removes any rows with any of :count, :rate, or
  :indicator-value."
  [coll]
  (let [fields [:count :rate :indicator-value]
        has-missing (fn [r] (some nil? (map r fields)))]
    (remove has-missing coll)))

(defn remove-missing-dset [dset]
  (let [cols (i/col-names dset)]
    (i/dataset cols (remove-missing (->maps dset)))))

(defn load-join-pivot [cts-dir data-file]
  (let [cts (read-cts-data cts-dir)
        indicator-data (read-indicator-data data-file)]
    (->> (join-all indicator-data cts)
      ->maps
      pivot-data
      remove-missing
      i/to-dataset)))

(defn aggregate-by-country
  "This aggregates the data after pivoting."
  ([dset] (aggregate-by-country dset :mean))
  ([dset by]
   (let [sum-counts (i/$rollup :sum :count [:country :year] dset)
         sum-rates (i/$rollup :sum :rate [:country :year] dset)
         sum-values (i/$rollup
                        by :indicator-value [:country :year] dset)]
     (reduce #(i/$join [[:country :year] [:country :year]] %1 %2)
             [sum-counts sum-rates sum-values]))))

(defn by-crime [dset crime-label]
  (i/$where {:crime {:$eq crime-label}} dset))

(defn aggregate-years-by-country
  ([dset] (aggregate-years-by-country dset :mean))
  ([dset by]
   (let [data-cols [:count :rate :indicator-value]]
     (->> [:count :rate :indicator-value]
       (map #(i/$rollup by % :country dset))
       (reduce #(i/$join [:country :country] %1 %2))))))

(comment

(reset)
(pst)

(def cts (d/read-cts-data "unodc-data"))
(def ag-lnd (d/read-indicator-data "ag.lnd/ag.lnd.totl.k2_Indicator_en_csv_v2.csv"))
(def ny-gnp (d/read-indicator-data "ny.gnp/ny.gnp.pcap.cd_Indicator_en_csv_v2.csv"))

(def by-ag-lnd
  (d/load-join-pivot
    "unodc-data"
    "ag.lnd/ag.lnd.totl.k2_Indicator_en_csv_v2.csv"))
(def by-ny-gnp
  (d/load-join-pivot
    "unodc-data"
    "ny.gnp/ny.gnp.pcap.cd_Indicator_en_csv_v2.csv"))
(def burglary (d/by-crime by-ny-gnp "CTS 2012 Burglary"))
(pprint (n-stat/spearmans :indicator-value :rate burglary))

(s/summary burglary)
(def h
  (c/histogram (i/sel burglary :cols :count)
                :nbins 30
                :title "Burglary Counts"
                :x-label "Burglaries"))
(def h (c/histogram (i/sel burglary :cols :indicator-value)
                    :nbins 30
                    :title "Land Areas"
                    :x-label "Land Areas"))
(i/view h)

(def burglary (i/$where {:crime {:$eq "CTS 2012 Burglary"}} by-ny-gnp))
  (def h (c/histogram (i/sel burglary :cols :indicator-value)
                      :nbins 30
                      :title "GNI per capita"
                      :x-label "GNI per capita"))

(s/summary (i/$where {:crime {:$eq "CTS 2012 Burglary"}} by-ag-lnd))
(s/summary (i/$where {:crime {:$eq "CTS 2012 Burglary"}} by-ny-gnp))

(def burglary (-> by-ny-gnp
                (d/by-crime "CTS 2012 Burglary")
                (d/aggregate-years-by-country :mean)))
(i/sel (i/$order :rate :desc burglary) :rows (range 5))
(i/sel (i/$order :indicator-value :desc burglary) :rows (range 5))

  )

