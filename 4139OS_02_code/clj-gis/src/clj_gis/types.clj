
(ns clj-gis.types)

(defrecord DeltaTemp
  [station lat lon delta-year delta-temp])

(defrecord DataPoint
  [lat lon value])

