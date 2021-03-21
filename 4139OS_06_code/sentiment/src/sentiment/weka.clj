
(ns sentiment.weka
  (:require [clojure.string :as str])
  (:import [java.util ArrayList]
           [weka.core Attribute DenseInstance Instances SparseInstance]
           [weka.filters Filter]
           [weka.filters.unsupervised.attribute Remove]))

(defn instances-attributes [f-index]
  (let [attrs (->> f-index
                (sort-by second)
                (map #(Attribute. (first %)))
                ArrayList.)
        review (Attribute. "review-rating" (ArrayList. ["+" "-"]))]
    (.add attrs review)
    [attrs review]))

(defn review->instance [attrs review]
  (let [i (SparseInstance. (.size attrs))]
    (doseq [[attr value] (map vector attrs (:feature-vec review))]
      (when-not (zero? value)
        (.setValue i attr (double value))))
    (.setValue i (last attrs) (:rating review))
    i))

(defn ->instances
  ([f-index review-coll]
   (->instances f-index review-coll "hotel-reviews"))
  ([f-index review-coll name]
   (let [[attrs review] (instances-attributes f-index)
         instances (Instances. name attrs (count review-coll))]
     (doseq [review review-coll]
       (let [i (review->instance attrs review)]
         (.add instances i)))
     (.setClass instances review)
     instances)))

(defn attr-n [instances attr-name]
  (->> instances
    (.numAttributes)
    range
    (map #(vector % (.. instances (attribute %) name)))
    (filter #(= (second %) (name attr-name)))
    ffirst))

(defn ->options [& opts]
  (into-array String (map str (flatten (remove nil? opts)))))

(defn random-seen [seed]
  (if (nil? seed)
    (.intValue (.getTime (java.util.Date.)))
    seed))

(defn analysis-parameter [parameter]
  (condp = (count parameter)
    3 `[~(first parameter) ~(second parameter)]
    4 (condp = (last parameter)
        :flag-true `[(when ~(second parameter)
                       ~(first parameter))]
        :flag-false `[(when-not ~(second parameter)
                        ~(first parameter))]
        :not-nil `[(when-not (nil? ~(second parameter))
                     [~(first parameter) ~(second parameter)])]
        :seq (let [name (second parameter)]
               (apply concat
                      (map-indexed (fn [i flag]
                                     `[~flag (nth ~name ~i)])
                                   (first parameter))))
        `[~(first parameter)
          (~(last parameter) ~(second parameter))])
    5 (condp = (nth parameter 3)
        :flag-equal `[(when (= ~(second parameter) ~(last parameter))
                        ~(first parameter))]
        :predicate `[(when ~(last parameter)
                       [~(first parameter) ~(second parameter)])])))

(defmacro defanalysis [a-name a-class a-method parameters]
  `(defn ~a-name
     [dataset# & {:keys ~(mapv second parameters)
                  :or ~(into {} (map #(vector (second %) (nth % 2))
                                     parameters))}]
     (let [options# (->options ~@(mapcat analysis-parameter parameters))]
       (doto (new ~a-class)
         (.setOptions options#)
         (. ~a-method dataset#)))))

(defn filter-attributes [dataset remove-attrs]
  (let [attrs (map inc (map attr-n remove-attrs))
        options (->options "-R" (str/join \, (map str attrs)))
        rm (doto (Remove.)
             (.setOptions options)
             (.setInputFormat dataset))]
    (Filter/useFilter dataset rm)))

(defn filter-class-index [instances]
  (let [i (.classIndex instances)]
    (if (>= 0 i)
      (let [rm (doto (Remove.)
                 (.setOptions (->options "-R" (str (inc i))))
                 (.setInputFormat instances))]
        (Filter/useFilter instances rm))
      instances)))

