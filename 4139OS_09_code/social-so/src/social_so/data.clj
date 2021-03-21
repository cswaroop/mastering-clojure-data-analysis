(ns social-so.data
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defrecord CountRank [count rank])
(defrecord UserInfo [user post q a])

(defn rank-on [user-property coll]
  (->> coll
    (sort-by user-property)
    reverse
    (map vector (range))))

(defn quantile-on [n user-property coll]
  (let [len (count coll)
        part-size (+ (quot len n)
                     (if (zero? (mod len n)) 0 1))]
    (partition-all part-size (sort-by user-property coll))))

(defn sum-count
  ([user-property] (partial sum-count user-property))
  ([user-property coll]
   (->> coll
     (map #(:count (user-property %)))
     (remove nil?)
     (reduce + 0))))

(defn save-data [filename data]
  (with-open [w (io/writer filename)]
    (json/write data w)))

(defn load-data [filename]
  (let [->info (fn [m]
                 (map->UserInfo
                   (assoc m
                          :post (map->CountRank (:post m))
                          :q (map->CountRank (:q m))
                          :a (map->CountRank (:a m)))))]
    (with-open [r (io/reader filename)]
      (doall (map ->info (json/read r :key-fn keyword))))))

