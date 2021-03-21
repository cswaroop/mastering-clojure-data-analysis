;;;; {{{1 ns
(ns ufo-data.bayes
  (:require [clojure.java.io :as io])
  (:import [cc.mallet.util.*]
           [cc.mallet.types InstanceList]
           [cc.mallet.pipe Input2CharSequence TokenSequenceLowercase
            CharSequence2TokenSequence SerialPipes SaveDataInSource
            Target2Label TokenSequence2FeatureSequence
            TokenSequenceRemoveStopwords
            FeatureSequence2AugmentableFeatureVector]
           [cc.mallet.pipe.iterator FileIterator]
           [cc.mallet.classify NaiveBayesTrainer]
           [java.io ObjectInputStream ObjectOutputStream]))

;;;; {{{1 Loading

(defn make-pipe-list []
  (SerialPipes.
    [(Target2Label.)
     (SaveDataInSource.)
     (Input2CharSequence. "UTF-8")
     (CharSequence2TokenSequence. #"\p{L}[\p{L}\p{P}]+\p{L}")
     (TokenSequenceLowercase.)
     (TokenSequenceRemoveStopwords.)
     (TokenSequence2FeatureSequence.)
     (FeatureSequence2AugmentableFeatureVector. false)]))

(defn add-input-directory [dir-name pipe]
  (doto (InstanceList. pipe)
    (.addThruPipe
      (FileIterator. (io/file dir-name)
                     #".*/([^/]*?)/\d+.txt$"))))

;;;; {{{1 Training

(defn train [instance-list]
  (.train (NaiveBayesTrainer.) instance-list))

;;;; {{{1 Classification

(defn classify [bayes instance-list]
  (.classify bayes instance-list))

;;;; {{{1 Validation

(defn validate1 [bayes instance]
  (let [c (.classify bayes instance)
        expected (.. c getInstance getTarget toString)
        actual (.. c getLabeling getBestLabel toString)]
    [expected actual]))

(defn confusion-matrix [classifier instances labels]
  (frequencies (map #(validate1 classifier %) instances)))

(defn map->map [f coll]
  (into {} (map #(vector % (f %)) coll)))

(defn validate [bayes instance-list]
  (let [labels (iterator-seq (.iterator (.getLabelAlphabet bayes)))]
    {:accuracy (.getAccuracy bayes instance-list)
     :precision (map->map #(.getPrecision bayes instance-list %) labels)
     :recall (map->map #(.getRecall bayes instance-list %) labels)
     :f1 (map->map #(.getF1 bayes instance-list %) labels)
     :matrix (confusion-matrix bayes instance-list)}))

;;;; {{{1 Full processing

(defn bayes [training-dir testing-dir]
  (let [pipe (make-pipe-list)
        training (add-input-directory training-dir pipe)
        testing (add-input-directory testing-dir pipe)
        classifier (train training)
        labels (iterator-seq
                 (.iterator (.getLabelAlphabet classifier)))
        c-matrix (confusion-matrix classifier testing labels)]
    {:bayes classifier
     :pipe pipe
     :confusion c-matrix}))

;;;; {{{1 Saving and loading classifiers

(defn save-classifier [bayes filename]
  (with-open [obj-out (ObjectOutputStream. (io/output-stream filename))]
    (.writeObject obj-out bayes)))

(defn load-classifier [filename]
  (with-open [obj-in (ObjectInputStream. (io/input-stream filename))]
    (.readObject obj-in)))
