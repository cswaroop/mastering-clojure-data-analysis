
(ns clj-gis.rolling-avg
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.core.reducers :as r]
    [clj-time.core :as clj-time]
    [clj-time.format :refer (formatter parse)]
    [clojure.data.csv :as csv]
    [me.raynes.fs :as fs]
    [clj-gis.filter-data :as fd]
    [clj-gis.locations :as loc]
    [clj-gis.types :refer :all]
    [clj-gis.util :as u]))

(def ^:dynamic *period* 10)
(def ^:dynamic *month* 8)
(def ^:dynamic *date-format* (formatter "yyyyMMdd"))

(defrecord WeatherRow
  [station wban date temp temp-count dewp dewp-count slp
   slp-count stp stp-count visibility vis-count wdsp
   wdsp-count max-wind-spd max-gust max-temp min-temp
   precipitation snow-depth rfshtt])

(defn parse-date
  ([ymd] (parse-date *date-format* ymd))
  ([date-formatter ymd]
   (parse date-formatter ymd)))

(defn parse-temp
  [temp]
  (Double/parseDouble (str/replace temp #"\*$" "")))

(defn normalize
  [row]
  (let [max-temp (parse-temp (:max-temp row))
        min-temp (parse-temp (:min-temp row))]
    (when-not (or (> max-temp 9000) (> min-temp 9999))
      (assoc row
             :date (parse-date (:date row))
             :max-temp max-temp
             :min-temp min-temp))))

(defn read-weather
  [filename]
  (with-open [f (io/reader filename)]
    (doall
      (->>
        f
        line-seq
        (r/drop 1)
        (r/map #(str/split % #"\s+"))
        (r/map #(apply ->WeatherRow %))
        (r/map normalize)
        (r/remove nil?)
        (into [])))))

(defn get-station-year
  [row]
  [(:station row) (:wban row) (clj-time/year (:date row))])

(defn sum
  [coll]
  (reduce + 0 coll))

(defn mean [coll]
  (/ (sum coll) (double (count coll))))

; [WeatherRow] -> [WeatherRow]
(defn only-month
  "1. We want to process only the observations for the month
  of July."
  [month coll]
  (r/filter #(= (clj-time/month (:date %)) month) coll))

(defn accum
  [m k v]
  (assoc m k (conj (m k []) v)))

(defn join-accum
  ([] {})
  ([m1 m2]
   (merge-with concat m1 m2)))

; [WeatherRow]
; {year [WeatherRow]}
; [[year double]]
; [WeatherRow] -> [[year double]]
(defn get-monthly-avgs
  "2. We want to average the observations for each year's July,
  so we'll have an average for July 2013, one for July 2012, one
  for July 2011, and so on."
  [weather-rows]
  (->> weather-rows
    (group-by #(clj-time/year (:date %)))
    (r/map (fn [[year group]]
             [year (mean (map :max-temp group))]))))

; [[year double]]
; [[year double]]
; [[year double]]
; [[[year double]]]
; [[[year double]]]
; [[[year double]]]
; [[year [double]]]
; [[year double]] -> [[year [double]]]
(defn get-windows
  "3. We want then to group these monthly averages into a
  rolling ten-year window. For example, one window will have
  the observations for 1950–1960. Another window will have
  observations for 1951–1961. And so on."
  ([month-avgs] (get-windows *period* month-avgs))
  ([period month-avgs]
   (->>
     month-avgs
     (into [])
     (sort-by first)
     (partition period 1)
     (r/filter #(> (count %) 1))
     (r/map #(vector (ffirst %) (map second %))))))

; [[year [double]]]
; [[year double]]
(defn average-window
  "4. We want the average of each of these windows for a
  climatic average temperature for July for that period."
  [windows]
  (r/map (fn [[start-year ws]] [start-year (mean ws)])
         windows))

; [double]
; double
(defn avg-diff
  "5. We can get the change in maximum temperature by subtracting
  the climatic average for the last window for a station from the
  average of its first window."
  [avgs]
  (- (last avgs) (first avgs)))

; [[year double]]
; [double]
; [double]
(defn only-avgs
  [coll]
  (into [] (r/map second coll)))

(defn get-station-locs
  ([] (get-station-locs loc/*ish-history-file*))
  ([history-file]
   (->>
     history-file
     fd/read-history
     (remove #(or (empty? (:lat %)) (empty? (:lon %))))
     (map #(vector [(:usaf %) (:wban %)]
                   [(/ (Double/parseDouble (:lat %)) 1000.0)
                    (/ (Double/parseDouble (:lon %)) 1000.0)]))
     (into {}))))
; [station-pair double]
; DeltaTemp
(defn into-delta-temps
  [locs pair]
  (let [[station avg] pair
        lat-lon (locs station)]
    (->DeltaTemp (str/join "-" station)
                 (first lat-lon) (second lat-lon)
                 nil avg)))

(defn get-file-station
  [filename]
  (vec (take 2 (str/split (first (fs/split-ext (fs/base-name filename))) #"-"))))

(defn on-second
  ([f] (partial on-second f))
  #_([tag f] (partial on-second tag f))
  ([f pair]
   (let [[x y] pair]
     [x (f y)]))
  ([tag f pair]
   (let [[x y] pair
         y' (f y)]
     (prn :on-second tag x (count (into [] y')))
     [x (f y)])))

; country-code
; file
; [file]
; [file]
; [[station-pair [file]]]
; [[station-pair [WeatherRow]]]
; country-code -> [[station-pair [WeatherRow]]]
(defn read-all-weather-data
  ([] (read-all-weather-data fd/*country-code*))
  ([country]
   (->> (fs/file country "*.op")
     fs/glob
     sort
     (u/group-seq get-file-station)
     (r/map (on-second #(r/mapcat read-weather %))))))

; [[station-pair [WeatherRow]]]
; [[station-pair [WeatherRow]]]
; [[station-pair [[year double]]]]
; [[station-pair [[year [double]]]]]
; [[station-pair [double]]]
; [[station-pair [double]]]
; [[station-pair double]]
; [DeltaTemp]
(defn transform->delta-temps
  ([weather-data]
   (transform->delta-temps
     loc/*ish-history-file* *month* *period* weather-data))
  ([history-file month period weather-data]
   (let [station-locs (get-station-locs history-file)]
     (->> weather-data
       (r/map (on-second #(only-month month %)))
       (r/map (on-second get-monthly-avgs))
       (r/map (on-second #(get-windows period %)))
       (r/map (on-second average-window))
       (r/map (on-second only-avgs))
       (r/remove #(zero? (count (second %))))
       (r/map (on-second avg-diff))
       (r/map #(into-delta-temps station-locs %))))))

(defn write-temperature-diffs
  [writer temp-diffs]
  (let [write-line (fn [writer line]
                     (csv/write-csv writer [line])
                     writer)]
    (csv/write-csv writer [[:station :lon :lat :delta-temp]])
    (r/reduce
      write-line writer
      (r/map #(vector (:station %) (:lon %) (:lat %) (:delta-temp %))
             temp-diffs))))

(defn save-temperature-diffs
  ([filename]
   (save-temperature-diffs filename loc/*ish-history-file*
                           fd/*country-code* *month* *period*))
  ([filename history-file country month period]
   (let [t-diffs (transform->delta-temps
                   history-file month period
                   (read-all-weather-data country))]
     (with-open [w (io/writer filename)]
       (write-temperature-diffs w t-diffs)))))

(defn vec-delta-temps
  ([] (vec-delta-temps
        loc/*ish-history-file* fd/*country-code* *month* *period*))
  ([history-file country month period]
   (->> (read-all-weather-data country)
     (transform->delta-temps history-file month period)
     (into []))))

