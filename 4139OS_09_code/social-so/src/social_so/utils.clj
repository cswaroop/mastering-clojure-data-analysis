(ns social-so.utils
  (:require [clojure.string :as str]))

(defn ->long
  ([x] (->long x nil))
  ([x default]
   (if (or (nil? x) (str/blank? x))
     default
     (try
       (Long/parseLong (str x))
       (catch Exception _
         default)))))

(defn tee
  ([tag] (partial tee tag))
  ([tag x]
   (println [tag x])
   (flush)
   x))

(defn to-lower [s] (str/lower-case (or s "")))

(defn all-top [n users]
  (->> [:post :q :a]
    (mapcat #(take n (sort-by (fn [u] (:rank (% u))) users)))
    (map #(vector (:user %) %))
    (into {})
    vals
    (sort-by :user)))

(defn rand-replace [m k v]
  (assoc (dissoc m (rand-nth (keys m))) k v))

(defn range-from [x] (map #(+ x %) (range)))

(defn sample [k coll]
  (->> coll
    (drop k)
    (map vector (range-from (inc k)))
    (filter #(<= (rand) (/ k (first %))))
    (reduce #(rand-replace %1 (first %2) (second %2))
            (into {} (map vector (range k) (take k coll))))
    (sort-by first)
    (map second)))

