(ns financial.csv-data
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format])
  (:use [financial types utils]))

(def date-format (time-format/formatter "d-MMM-YY"))

(defn row->StockData [row]
  (let [[date open high low close vol] row]
    (->StockData (time-format/parse date-format date)
                 (->double open)
                 (->double high)
                 (->double low)
                 (->double close)
                 (->long vol))))

(defn read-stock-prices [filename]
  (with-open [f (io/reader filename)]
    (doall (map row->StockData (drop 1 (csv/read-csv f))))))

