
(ns ufo-data.tm
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import [cc.mallet.util.*]
           [cc.mallet.types InstanceList]
           [cc.mallet.pipe
            Input2CharSequence TokenSequenceLowercase
            CharSequence2TokenSequence SerialPipes
            TokenSequenceRemoveStopwords
            TokenSequence2FeatureSequence]
           [cc.mallet.pipe.iterator ArrayIterator]
           [cc.mallet.topics ParallelTopicModel]
           [java.io FileFilter]
           [java.util Formatter Locale]))

(defn make-pipe-list []
  (InstanceList.
    (SerialPipes.
      [(Input2CharSequence. "UTF-8")
       (CharSequence2TokenSequence. #"\p{L}+")
       (TokenSequenceLowercase.)
       (TokenSequenceRemoveStopwords. false false)
       (TokenSequence2FeatureSequence.)])))

(defn add-strings [instance-list coll]
  (.addThruPipe instance-list
                (ArrayIterator. (into-array coll)))
  instance-list)

(defn train-model
  ([instances] (train-model 100 4 50 instances))
  ([num-topics num-threads num-iterations instances]
   (doto (ParallelTopicModel. num-topics 1.0 0.01)
     (.addInstances instances)
     (.setNumThreads num-threads)
     (.setNumIterations num-iterations)
     (.estimate))))

(defn get-topic-words
  "Returns the words and weights as they apply to the given
  topic."
  [model instances topic-n]
  (let [topic-words (.getSortedWords model)
        data-alpha (.getDataAlphabet instances)]
    (map #(vector (.lookupObject data-alpha (.getID %))
                  (.getWeight %))
         (iterator-seq (.. topic-words (get topic-n)
                         iterator)))))

(defn topic-word-reports
  ([model instances take-n]
   (->> model
     .numTopics
     range
     (map #(get-topic-words model instances %))
     (map #(take take-n %))
     pp/pprint))
  ([model instances take-n filename]
   (with-open [out (io/writer filename)]
     (binding [*out* out]
       (topic-word-reports model instances take-n)
       (flush)))))

(defn get-highest-inst [model ilist topic-id]
  (let [get-inst (fn [[prev-i prev-max :as prev-pair] n]
                   (let [p (.getTopicProbabilities model n)
                         ip (aget p topic-id)]
                     (if (> ip prev-max)
                       [n ip]
                       prev-pair)))]
    (reduce get-inst [-1 Integer/MIN_VALUE]
            (range (count ilist)))))

(defn rank-instances [model data topic-id]
  (let [get-p (fn [n]
                [(aget (.getTopicProbabilities model n) topic-id)
                 (nth data n)])]
    (->> data count range (map get-p) (sort-by first) reverse)))

(defn rank-report [model data topic-id n]
  (->>
    (rank-instances model data topic-id)
    (take n)
    (map #(vector (first %) (:description (second %))))
    pp/pprint))

(defn make-topic-info
  "Creates a map of information about the instance and topic.
  This should pull out everything needed for the basic
  visualizations."
  ([model n topic-n]
   (make-topic-info model (.getTopicProbabilities model n)
                    n topic-n))
  ([model topic-dist n topic-n]
   (let [path (.. model getData (get n) instance
                getName getPath)]
     {:instance-n n
      :path (str/replace path #".*/([^/]*).txt" "$1")
      :topic-n topic-n
      :distribution (aget topic-dist topic-n)})))

