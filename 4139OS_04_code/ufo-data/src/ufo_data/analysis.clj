
(ns ufo-data.analysis
  (:require [ufo-data.text :as t]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.string :as str]
            [incanter.core :as i]
            [incanter.stats :as s]))

(defn get-shape
  "This gets the shape, or if it doesn't have a value, the description."
  [sighting]
  (let [shape (:shape sighting)]
    (if (or (str/blank? shape) (= shape " unknown") (= shape " other"))
      (:description sighting)
      shape)))

(defn get-shape-bag
  "This returns the bag of words for a sighting."
  [sighting]
  (->> sighting
    get-shape
    t/normalize
    t/tokenize
    (remove t/*stop-list*)
    set))

(defn get-date-shape
  "This projects the UFO Sighting onto a date long and shape
  field"
  [sighting]
  (if-let [sighted-at (coerce/to-long (:sighted-at sighting))]
    (->> sighting
      :shape
      t/normalize
      t/tokenize
      (map #(hash-map :sighted-at sighted-at :shape %)))
    []))

(defn get-all-date-shapes
  "This returns the date and shape data for all records."
  [coll]
  (remove empty? (mapcat get-date-shape coll)))

(defn get-shape-freqs
  "This computes the :shape field's frequencies. This also
  removes any items with a frequency less than min-freq."
  [coll min-freq]
  (->> coll
    (map :shape)
    (remove str/blank?)
    (map t/normalize)
    (mapcat t/tokenize)
    frequencies
    (remove #(< (second %) min-freq))
    (sort-by second)
    reverse
    (map #(zipmap [:shape :count] %))
    (into [])))

(defn get-descriptions
  [coll]
  (->> coll
    (map :description)
    (map t/normalize)
    (mapcat t/tokenize)
    (remove t/*stop-list*)))

(defn get-descr-counts
  [coll min-freq]
  (->> coll
    get-descriptions
    frequencies
    (remove #(< (second %) min-freq))
    (sort-by second)
    reverse
    (map #(zipmap [:descr :count] %))))

(defn get-date-shape-ids
  "This projects the UFO sightings onto an ID, a date long, and a shape term."
  [sighting]
  (if-let [sighted-at (coerce/to-long (:sighted-at sighting))]
    (let [id (:id sighting)]
      (map #(vector id sighted-at %) (get-shape-bag sighting)))
    []))

(defn timestamp->year [ts]
  (time/year (coerce/from-long ts)))

(defn timestamp->month [ts]
  (time/month (coerce/from-long ts)))

(defn timestamp->decade [ts]
  (* (long (/ (timestamp->year ts) 10.0)) 10))

(defn on-second [f]
  (fn [[a b]] (vector a (f b))))

(defn group-by-year
  "This groups the observations by year."
  [coll]
  (group-by #(timestamp->year (:sighted-at %)) coll))

(defn get-year-counts
  "This takes data grouped by year and returns the years and
  counts."
  [by-year]
  (map #(zipmap [:year :count] %)
       (map (on-second count)
            by-year)))

(defn group-by-month
  [coll]
  (->> coll
    (filter :sighted-at)
    (group-by (comp time/month :sighted-at))
    (map (on-second count))
    (map #(zipmap [:month :count] %))))

(defn test-seasons
  "This takes a seq of maps with keys :year, :month, :season, and :count."
  [data]
  (let [m (i/to-matrix (i/dataset [:year :month :season :count] data))
        [f s t fr] (i/group-on m 2 :cols 3)]
    [(s/t-test f :y s) (s/t-test f :y t) (s/t-test f :y fr)
     (s/t-test s :y t) (s/t-test s :y fr)
     (s/t-test t :y fr)]))


