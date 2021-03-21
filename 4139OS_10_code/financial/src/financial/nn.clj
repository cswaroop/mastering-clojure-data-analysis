(ns financial.nn
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [clojure.java.io :as io]
            [enclog.nnets :as nnets]
            [enclog.training :as training]
            [financial.utils :as u]
            [financial.validate :as v])
  (:import [org.encog.neural.networks PersistBasicNetwork]))

(def periods [(time/days 1)
              (time/days 2)
              (time/days 3)
              (time/days 4)
              (time/days 5)
              (time/days (* 7 2))
              (time/days (* 7 3))
              (time/days 30)
              (time/days (* 30 2))
              (time/days (* 30 6))
              (time/days 365)
              ])
(def out-periods [(time/days 1)
                  (time/days 2)
                  (time/days 5)])

(defn index-by [key-fn coll]
  (into {} (map #(vector (key-fn %) %) coll)))

(defn get-stock-date [stock-index date]
  (let [date (.withTimeAtStartOfDay date)]
    (if-let [price (stock-index date)]
      price
      (if (<= (time/year date) 1990)
        nil
        (get-stock-date
          stock-index (time/minus date (time/days 1)))))))

(defn make-price-vector [stock-index article date-op]
  (let [pub-date (:pub-date article)
        base-price (:close (get-stock-date stock-index pub-date))
        price-feature
        (fn [period]
          (let [date-key (date-op pub-date period)]
            (if-let [stock (get-stock-date stock-index date-key)]
              (/ (:close stock) base-price)
              0.0)))]
    (vec (remove nil? (map price-feature periods)))))

(defn make-feature-vector [stock-index vocab article]
  (let [freqs (:text article)
        token-features (map #(freqs % 0.0) (sort vocab))
        price-features (make-price-vector
                         stock-index article time/minus)]
    (vec (concat token-features price-features))))

(defn make-training-vector [stock-index article]
  (vec (make-price-vector stock-index article time/plus)))

(defn make-training-set [stock-index vocab articles]
  (let [make-pair
        (fn [article]
          {:input (make-feature-vector stock-index vocab article)
           :outputs (zipmap periods
                            (make-training-vector stock-index article))})]
    (map make-pair articles)))

(defn make-network [vocab-size hidden-nodes]
  (nnets/network (nnets/neural-pattern :feed-forward)
                 :activation :sigmoid
                 :input (+ vocab-size (count periods))
                 :hidden [hidden-nodes]
                 :output 1))

(defn activated [act-fn output]
  (let [a (double-array 1 [output])]
    (.activationFunction act-fn a 0 1)
    a))

(defn build-data [nnet period training-set]
  (let [act (.getActivation nnet (dec (.getLayerCount nnet)))
        output (mapv #(activated act (get (:outputs %) period))
                     training-set)]
    (training/data :basic-dataset
                   (mapv :input training-set)
                   output)))

(defn train-for
  ([nnet period training-set]
   (train-for nnet period training-set 0.01 500 []))
  ([nnet period training-set error-tolerance iterations strategies]
   (let [data (build-data nnet period training-set)
         trainer (training/trainer :back-prop
                                   :network nnet
                                   :training-set data)]
     (training/train trainer error-tolerance iterations strategies)
     nnet)))

(defn make-train [vocab-size hidden-count period coll]
  (let [nn (make-network vocab-size hidden-count)]
    (train-for nn period coll 0.01 100 [])
    nn))

(defn run-network [nnet input]
  (let [input (double-array (count input) input)
        output (double-array (.getOutputCount nnet))]
    (.compute nnet input output)
    output))

(defn test-on [nnet period test-set]
  "Runs the net on the test set and calculates the SSE."
  (let [act (.getActivation nnet (dec (.getLayerCount nnet)))
        sqr (fn [x] (* x x))
        error (fn [{:keys [input outputs]}]
                (- (first (activated act (get outputs period)))
                   (first (run-network nnet input))))]
    (reduce + 0.0 (map sqr (map error test-set)))))

(defn accum
  ([] [])
  ([v x] (conj v x)))

(defn x-validate [vocab-size hidden-count period coll]
  (v/k-fold #(make-train vocab-size hidden-count period %)
            #(test-on %1 period %2)
            accum
            10
            coll))

(def ^:dynamic *hidden-counts* [5 10 25 50])

(defn explore-point [vocab-count period hidden-count training]
  (println period hidden-count)
  (let [error (x-validate vocab-count hidden-count period training)]
    (println period hidden-count '=> \tab (u/mean error) \tab error)
    (println)
    error))

(defn final-eval [vocab-size period hidden-count training-set test-set]
  (let [nnet (make-train vocab-size hidden-count period training-set)
        error (test-on nnet period test-set)]
    {:period period
     :hidden-count hidden-count
     :nnet nnet
     :error error}))

(defn explore-params
  ([error-ref vocab-count training]
   (explore-params
     error-ref vocab-count training *hidden-counts* 0.2))
  ([error-ref vocab-count training hidden-counts test-ratio]
   (let [[test-set dev-set] (u/rand-split training test-ratio)
         search-space (for [p out-periods, h hidden-counts] [p h])]
     (doseq [pair search-space]
       (let [[p h] pair,
             error (explore-point vocab-count p h dev-set)]
         (dosync
           (commute error-ref assoc pair error))))
     (println "Final evaluation against the test set.")
     (let [[period hidden-count]
           (first (apply min-key #(u/mean (second %)) @error-ref))
           final (final-eval
                   vocab-count period hidden-count dev-set test-set)]
       (dosync
         (commute error-ref assoc :final final))))
   @error-ref))

(defn save-network [nn filename]
  (with-open [s-out (io/output-stream filename)]
    (.save (PersistBasicNetwork.) s-out nn)))

(defn load-network [filename]
  (with-open [s-in (io/input-stream filename)]
    (.read (PersistBasicNetwork.) s-in)))
