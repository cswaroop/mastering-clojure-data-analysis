
(ns sentiment.tokens
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [opennlp.tools.tokenize SimpleTokenizer]
           [opennlp.tools.postag POSModel POSTaggerME]))

(defn tokenize [s]
  (map (memfn toLowerCase)
       (seq
         (.tokenize SimpleTokenizer/INSTANCE s))))

(defn get-feature-val [f-index f-vec feature]
  (if-let [i (f-index feature)]
    (nth f-vec i)
    0))

(defn inc-feature [f-index f-vec feature]
  (if-let [i (f-index feature)]
    [f-index, (assoc f-vec i (inc (nth f-vec i)))]
    (let [i (count f-index)]
      [(assoc f-index feature i), (assoc f-vec i 1)])))

(defn ->feature-vec [f-index features]
  (reduce #(inc-feature (first %1) (second %1) %2)
          [f-index (vec (repeat (count f-index) 0))]
          features))

(defn accum-features [state features]
  (let [[index accum] state
        [new-index feature] (->feature-vec index features)]
    [new-index (conj accum feature)]))

(defn pad-to [f-index f-vec]
  (vec (take (count f-index) (concat f-vec (repeat 0)))))

(defn ->features [feature-seq]
  (let [[f-index f-vecs] (reduce accum-features [{} []] feature-seq)]
    [f-index (map #(pad-to f-index %) f-vecs)]))

(def unigrams identity)

(defn n-grams [n coll]
  (map #(str/join " " %) (partition n 1 coll)))
(defn bigrams [coll] (n-grams 2 coll))
(defn trigrams [coll] (n-grams 3 coll))

(defn with-pos [model coll]
  (map #(str/join "_" [%1 %2])
       coll
       (.tag model (into-array coll))))

(defn read-me-tagger [filename]
  (->> filename
    io/input-stream
    POSModel.
    POSTaggerME.))


