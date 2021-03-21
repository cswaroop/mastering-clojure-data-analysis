
(ns sentiment.analysis
  (:require [sentiment.weka :as w]
            [sentiment.x-valid :as xv])
  (:import [weka.classifiers.functions Logistic]
           [weka.classifiers.bayes NaiveBayes]))

(defn run-instance [classifier instances instance]
  (let [dist (.distributionForInstance classifier instance)
        i (first (apply max-key second (map vector (range) dist)))]
    (.. instances classAttribute (value i))))

(defn run-classifier [classifier instances]
  (map #(run-instance classifier instances %) instances))

(defn run-k-fold [trainer]
  (fn [f-index coll]
    (let [do-train (fn [xs]
                     (let [is (w/->instances f-index xs)]
                       (trainer is)))
          do-test (fn [classifier xs]
                    (->> xs
                      (w/->instances f-index)
                      w/filter-class-index
                      (run-classifier classifier)
                      (xv/compute-error (map :rating xs))
                      vector))]
      (xv/k-fold do-train do-test concat 10 coll))))

(w/defanalysis
  train-logistic Logistic buildClassifier
  [["-D" debugging false :flag-true]
   ["-R" ridge nil :not-nil]
   ["-M" max-iterations -1]])

(def k-fold-logistic (run-k-fold train-logistic))

(w/defanalysis
  train-naive-bayes NaiveBayes buildClassifier
  [["-K" kernel-density false :flag-true]
   ["-D" discretization false :flag-true]])

(def k-fold-naive-bayes (run-k-fold train-naive-bayes))

