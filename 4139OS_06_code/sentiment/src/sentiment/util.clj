
(ns sentiment.util)

(defn mean [coll]
  (let [accum (fn [[sum size] n]
                   [(+ sum n) (inc size)])
        calc-mean (fn [[sum size]] (/ sum size))]
    (calc-mean
      (reduce accum [0 0] coll))))

(defn pad-to
  ([n coll] (pad-to n coll nil))
  ([n coll value] (take n (concat coll (repeat value)))))

(defn on-second [f pair]
  "Functor for a vector pair."
  (let [[a b] pair]
    [a (f b)]))

