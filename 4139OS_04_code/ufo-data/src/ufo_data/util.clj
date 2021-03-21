(ns ufo-data.util)

(defn select-one
  "This takes a set of indexes and a maximum index and it returns a new index,
  plus the seen set with that index added."
  [seen n]
  (let [i (rand-int n)]
    (if (contains? seen i)
      (recur seen n)
      [(conj seen i) i])))

(defn select-accum [p state n]
  "Select an item between 0-p, given the state (items seen before and an
  accumulator of items. The last parameter is ignored."
  (let [[seen accum] state
        [seen2 i] (select-one seen p)]
    [seen2 (conj accum i)]))

(defn select-from-group
  "This selects two non-overlapping groups of k items from this group."
  [group k]
  (let [select-accum- (partial select-accum (count group))
        range-k (range k)
        [g1-seen g1] (reduce select-accum- [#{}     []] range-k)
        [g2-seen g2] (reduce select-accum- [g1-seen []] range-k)]
    [g1 g2]))

(defn get-nths
  "This takes a collection and a collection of indexes and returns the original
  items."
  [coll indexes]
  (map #(nth coll %) indexes))

(defn transpose [m]
  (apply mapv vector m))

(defn all? [p coll]
  (reduce #(and %1 %2) true (map p coll)))

(defn create-sets
  "This takes a map mapping keys to groups of items and a size for output sets,
  and it returns two more groups of approximately size k. The returned groups
  are made up of an equal number of random items from both other groups."
  [groups k]
  (let [k2 (/ k 2.0)
        ks (keys groups)
        key-groups (fn [k iss]
                     (let [g (get groups k)]
                       (mapv #(get-nths g %) iss)))]
    (when (all? #(> (count %) k2) (vals groups))
      (->> ks
        (map #(select-from-group (get groups %) k2))
        (mapv key-groups ks)
        transpose
        (mapv #(zipmap ks %))))))

(defn on-first [f [x y]] [(f x) y])
(defn on-second [f [x y]] [x (f y)])
(defn on-both [f [x y]] [(f x) (f y)])

(defn map-vals [f m] (into {} (map #(on-second f %) m)))

