(ns financial.validate
  (:require [financial.utils :as u]))

(defn k-fold
  "This performs k-fold cross-validation.

    `train` is a function that takes a training set and returns a value;
    `error` is a function that takes the output of `train` and a test set, and
          it returns the error rate;
    `combine` joins the error rates together;
    `k` (optional, default is 10) is the number of folds to use in
          validation; and
    `input-data` is the input."
  ([train error combine input-data]
   (k-fold train error combine 10 input-data))
  ([train error combine k input-data]
   (->> input-data
     shuffle
     (u/partition-folds k)
     u/step-folds
     (map (fn [[v t]] [v (train t)]))
     (map (fn [[v t]] (error t v)))
     (reduce combine (combine)))))
