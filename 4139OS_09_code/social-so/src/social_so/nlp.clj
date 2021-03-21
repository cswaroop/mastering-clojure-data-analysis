(ns social-so.nlp
  (:import [cc.mallet.types Instance InstanceList]
           [cc.mallet.pipe
            Input2CharSequence TokenSequenceLowercase
            CharSequence2TokenSequence SerialPipes
            Target2Label FeatureSequence2FeatureVector
            TokenSequenceRemoveStopwords
            TokenSequence2FeatureSequence]
           [cc.mallet.pipe.iterator ArrayDataAndTargetIterator]
           [cc.mallet.classify NaiveBayes NaiveBayesTrainer Trial]
           [cc.mallet.classify.evaluate ConfusionMatrix]))

(comment

(reset)
(def s (:post-infos system))
(def ilist (n/post-info-instances s))
(def tt-sets (n/split-sets 0.2 ilist))
(def bayes (n/bayes-train (:training tt-sets)))
(def trial (n/trial bayes (:test tt-sets)))
(println (confusion-matrix trial))
(.getF1 trial "not")
(.getF1 trial "accepted")

(pst)

  )

(defn make-pipe []
  (SerialPipes.
    [(Target2Label.)
     (Input2CharSequence. "UTF-8")
     (CharSequence2TokenSequence.
       #"\p{L}[\p{L}\p{P}]+\p{L}")
     (TokenSequenceLowercase.)
     (TokenSequenceRemoveStopwords. false false)
     (TokenSequence2FeatureSequence.)
     (FeatureSequence2FeatureVector.)]))

(defn accepted-tag [post-info]
  (if (:accepted-for post-info) "accepted" "not"))

(defn post-info-iterator [post-infos]
  (ArrayDataAndTargetIterator.
    (into-array (map :body-text post-infos))
    (into-array (map accepted-tag post-infos))))

(defn post-info-instances [post-infos]
  (doto (InstanceList. (make-pipe))
    (.addThruPipe (post-info-iterator post-infos))))

(defn split-sets [test-ratio ilist]
  (let [split-on (double-array [test-ratio (- 1.0 test-ratio)])
        [test-set training-set] (.split ilist split-on)]
    {:test test-set, :training training-set}))

(defn bayes-train [ilist] (.train (NaiveBayesTrainer.) ilist))
(defn trial [classifier ilist] (Trial. classifier ilist))
(defn confusion-matrix [trial] (str (ConfusionMatrix. trial)))

