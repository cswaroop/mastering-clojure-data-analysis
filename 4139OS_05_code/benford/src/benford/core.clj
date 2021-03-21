
(ns benford.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.data.csv :as csv]
            [incanter.stats :as s]))

(defn first-digit [n]
  (Integer/parseInt (re-find #"\d" (str n))))

(defn first-digit-freq [coll]
  (frequencies (map first-digit coll)))

(defn freq->ratio [m]
  (let [total (reduce + 0.0 (map second m))]
    (into {} (map #(vector (first %) (/ (second %) total)) m))))

(defn benford [d]
  (Math/log10 (+ 1.0 (/ 1.0 (float d)))))

(defn sum [coll]
  (reduce + 0.0 coll))

(defn sample [coll k]
  (if (<= (count coll) k)
    coll
    (let [coll-size (count coll)]
      (loop [seen #{}]
        (if (>= (count seen) k)
          (map #(nth coll %) (sort seen))
          (recur (conj seen (rand-int coll-size))))))))

(defn benford-sample [coll k]
  (s/benford-test (sample coll k)))

(defn x-sqr [expected-ratios observed]
  (let [total (sum observed)
        f (fn [e o]
            (let [n (- o e)]
              (/ (* n n) e)))]
    (sum (map f (map #(* % total) expected-ratios) observed))))

(defn x-sqr-v [expected-ratios observed]
  (let [total (sum observed)
        f (fn [e o]
            (let [n (- o e)]
              (println "\te =" e " o =" o " (o - e) =" n " (o - e)**2 = " (* n n) " (o-e)**2/e = " (/ (* n n) e))
              (/ (* n n) e)))
        es (map #(* % total) expected-ratios)]
    (println "os =" observed)
    (println "es =" es)
    (sum (map f (map #(* % total) expected-ratios) observed))))

(defn benford-test [coll]
  (let [freqs (first-digit-freq coll)
        digits (range 1 10)]
    (x-sqr (map benford digits) (map freqs digits))))

(defn cmp-benford [coll]
  [(benford-test coll) (:X-sq (s/benford-test coll))])

(defn read-csv [filename]
  (with-open [f (io/reader filename)]
    (let [[row & reader] (csv/read-csv f)
          header (map keyword (map #(str/replace % \space \-) row))]
      (doall
        (map #(zipmap header %) reader)))))

(defn read-databank [filename]
  (let [year-keys (map keyword (map str (range 1960 2013)))]
    (->> filename
      read-csv
      (mapcat #(map (fn [f] (f %)) year-keys))
      (remove empty?)
      (map #(Double/parseDouble %))
      (remove zero?))))

(defn make-rand-range-fn [coll]
  (let [data-min (reduce min coll), data-max (reduce max coll),
        data-range (- data-max data-min)]
    (fn [] (+ data-min (rand-int data-range)))))

(defn swap-random [coll swapped-set rand-index rand-value n]
  (reduce (fn [[v s] [i x]]
            [(assoc v i x) (conj s i)])
          [coll swapped-set]
          (map vector
               (repeatedly n rand-index)
               (repeatedly n rand-value))))

(defn make-fraudulent
  ([data] (make-fraudulent data 1 0.05 1000))
  ([data block sig-level k]
   (let [get-rand (make-rand-range-fn data)]
     (loop [v (vec (sample data k)), benford (s/benford-test v),
            n 0, ps [], swapped #{}]
       (println n \. (:p-value benford))
       (if (< (:p-value benford) sig-level)
         {:n n, :benford benford, :data v, :p-history ps,
          :swapped swapped}
         (let [[new-v new-swapped]
               (swap-random
                 v swapped #(rand-int k) get-rand block)
               benford (s/benford-test new-v)]
           (recur new-v benford (inc n)
                  (conj ps (:p-value benford))
                  new-swapped)))))))


