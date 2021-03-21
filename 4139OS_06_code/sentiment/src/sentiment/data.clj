
(ns sentiment.data
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.core.reducers :as r]
            [sentiment.tokens :as t]
            [sentiment.util :as util]))

(defrecord HotelReview
  [rating date title review feature-vec])

(defn read-data [filename]
  (let [ratings #{"+" "-"}]
    (doall
      (with-open [f (io/reader filename)]
        (->>
          (csv/read-csv f :separator \tab)
          (filter #(ratings (first %)))
          (map butlast)
          (map #(util/pad-to 5 %))
          (map #(apply ->HotelReview %))
          (group-by :rating))))))

(defn sample [coll]
  (let [size (reduce min Integer/MAX_VALUE
                     (map count
                          (vals coll)))]
    (into {}
          (map #(util/on-second (fn [coll] (take size (shuffle coll))) %)
               coll))))

(defn get-text [review]
  (str (:title review) \space (:review review)))

(defn update-feature-vec [review feature-vec] (assoc review :feature-vec feature-vec))

(defn add-features [feature-f reviews]
  (let [update-feature-vec (fn [review feature-vec]
                             (assoc review :feature-vec feature-vec))]
    (->> reviews
      (r/map get-text)
      (r/map t/tokenize)
      (r/map feature-f)
      (into [])
      t/->features
      (util/on-second #(map update-feature-vec reviews %)))))

