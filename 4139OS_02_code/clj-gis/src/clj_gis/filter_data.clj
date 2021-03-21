
(ns clj-gis.filter-data
  (:require
    [clojure.string :as str]
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [me.raynes.fs :as fs]
    [clj-gis.locations :as loc]
    [clj-gis.util :refer (ensure-dir)]))

(defrecord IshHistory
  [usaf wban station_name country fips state call
   lat lon elevation begin end])

(def ^:dynamic *country-code* "US")

(defn read-history
  "Read the station history file."
  [filename]
  (with-open [f (io/reader filename)]
    (doall
      (->> (csv/read-csv f)
        (drop 1)
        (map #(apply ->IshHistory %))))))

(defn get-station-set
  "Create a set of all stations in a country."
  [country histories]
  (set (map #(vector (:usaf %) (:wban %))
            (filter #(= (:country %) country)
                    histories))))

(defn filter-data-files
  "Read the history file and copy data files matching the
  country code into a new directory."
  [ish-history-file data-dir country-code]
  (let [history (read-history ish-history-file)
        stations (get-station-set country-code history)]
    (ensure-dir (fs/file country-code))
    (doseq [filename (fs/glob (str data-dir "*.op"))]
      (let [base (fs/base-name filename)
            station (vec (take 2 (str/split base #"-")))]
        (when (contains? stations station)
          (fs/copy filename (fs/file country-code base)))))))

