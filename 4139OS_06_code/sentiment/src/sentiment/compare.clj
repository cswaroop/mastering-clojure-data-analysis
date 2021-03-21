
(ns sentiment.compare
  (:require [sentiment.analysis :as a]
            [sentiment.data :as d]
            [sentiment.tokens :as t]
            [sentiment.x-valid :as x]))

(def classifiers
  {:naive-bayes a/k-fold-naive-bayes
   :maxent a/k-fold-logistic})
(def feature-factories
  {:unigram t/unigrams
   :bigram t/bigrams
   :trigram t/trigrams
   :pos (let [pos-model (t/read-me-tagger "data/en-pos-maxent.bin")]
          (fn [ts] (t/with-pos pos-model ts)))})

(defn do-class [f-key f-index features c-info]
  (let [[c-key c] c-info, k [c-key f-key]]
    (println k)
    [k (x/mean-error (c f-index features))]))

(defn do-features [docs classifiers f-info]
  (let [[f-key f] f-info
        [f-index features] (d/add-features f docs)]
    (map #(do-class f-key f-index features %)
         classifiers)))

(defn test-suite [docs]
  (into {} (mapcat #(do-features docs classifiers %)
                   feature-factories)))

;;;; From one run:
(def results
  {[:naive-bayes :unigram] {:precision 0.6387908041477204, :recall 0.6606976151466369},
   [:maxent      :unigram] {:precision 0.5160820931196213, :recall 0.5345282018184662},
   [:naive-bayes :bigram]  {:precision 0.5398953706026077, :recall 0.9059780955314636},
   [:maxent      :bigram]  {:precision 0.5269489675760269, :recall 0.5587860971689225},
   [:naive-bayes :trigram] {:precision 0.5205729126930236, :recall 0.8654873192310333},
   [:maxent      :trigram] {:precision 0.5485683232545853, :recall 0.5988996088504791},
   [:naive-bayes :pos]     {:precision 0.6042523086071014, :recall 0.6897578835487366},
   [:maxent      :pos]     {:precision 0.5004809111356735, :recall 0.5116415798664093}})

