
(ns nullh.geo
  (:require [geocoder.google :as goog]
            [geocoder.bing :as bing]
            [geocoder.geonames :as geon]
            [geo.core :as geo]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(def geocoders [{:name :google,
                 :geocode-address goog/geocode-address,
                 :location goog/location}
                ; {:name :bing, :geocode-address bing/geocode-address, :location bing/location}
                {:name :geonames,
                 :geocode-address geon/geocode-address,
                 :location geon/location}])

(defn point->geojson [point]
  (when (and (not (nil? point)) (geo/point? point))
    {:type "Point", :coordinates [(geo/point-x point)
                                  (geo/point-y point)]}))

(defn geocode-with [agency-name geocode-address location]
  (when-let [address (first (geocode-address agency-name))]
    (point->geojson (location address))))

(defn geocode-all [agency-names]
  (let [gc-nil (fn [geocoder [n p :as geo-info]]
                 (if (nil? p)
                   [n (geocode-with n
                                    (:geocode-address geocoder)
                                    (:location geocoder))]
                   geo-info))
        try-geo (fn [names geocoder]
                  (println (:name geocoder))
                  (try (map #(gc-nil geocoder %) names)
                    (catch Exception _ names)))]
    (reduce try-geo (map #(vector % nil) agency-names) geocoders)))

(defn save-geocoding [filename geo-data]
  (with-open [w (io/writer filename)]
    (csv/write-csv w [[:agency_name :lng :lat]])
    (->> geo-data
      (map #(concat [(first %)] (:coordinates (point->geojson (second %)))))
      (csv/write-csv w))))

(defn read-geocoding [filename])

