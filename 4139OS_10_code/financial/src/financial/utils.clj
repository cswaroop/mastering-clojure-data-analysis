(ns financial.utils
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn ls [dir] (map #(io/file dir %) (fs/list-dir dir)))

(defn chext [filename ext]
  (let [ext (if (.startsWith ext ".") ext (str \. ext))]
    (io/file (fs/parent filename) (str (fs/name filename) ext))))

(defn coerce [f v d]
  (try
    (f v)
    (catch Exception e
      d)))

(defn ->double [v] (coerce #(Double/parseDouble %) v 0.0))
(defn ->long [v] (coerce #(Long/parseLong %) v 0))

(defn over [k f m] (assoc m k (f (get m k))))

(defn partition-folds
  "This returns partitions of row indexes. This takes the
  results of dividing by k and spreads the modulus over the
  partitions.

  `k` is the number of partitions;
  `coll` is the dataset."
  [k coll]
  (let [c (count coll), n (Math/floor (/ c k)), r (mod c k),
        [larger right] (split-at (* (inc n) r) coll)]
    (concat (partition-all (inc n) larger)
            (partition-all n right))))

(defn step-folds-seq
  "This builds the sequence use by step-folds."
  [folds steps]
  (lazy-seq
    (when-let [[s & ss] (seq steps)]
      (let [[prefix [validation & suffix]] (split-at s folds)
            training (vec (flatten (concat prefix suffix)))
            test-set (vec validation)
            current [test-set training]]
        (cons current (step-folds-seq folds ss))))))

(defn step-folds
  "This takes a dataset and a sequence of sequence of indices
  (the folds), and it returns a sequence of pairs of
  validation subset and training subset. It walks through each
  subsequence of the folds and pulls that item out as the
  validation set and uses the rest of the items as the
  training set. It returns the slices from the original
  dataset. Essentially, this reifies the fold partitions used
  in K-fold cross-validation."
  [folds]
  (step-folds-seq folds (range (count folds))))

(defn mean
  [coll]
  (float (/ (reduce + 0.0 coll) (count coll))))

(defn rand-split [coll by]
  (let [indexes (count coll)
        [indexes-a indexes-b] (split-at (long (* indexes by))
                                        (shuffle (range indexes)))]
    [(map #(nth coll %) indexes-a), (map #(nth coll %) indexes-b)]))
