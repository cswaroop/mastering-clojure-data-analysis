
(ns ufo-data.system
  (:require [ufo-data.model :as m]
            [ufo-data.analysis :as a]
            [ufo-data.bayes :as b]
            [ufo-data.corpus :as c]
            [ufo-data.util :refer :all]
            ))

(defn system []
  {:data nil})

(defn start [system]
  system
  #_
  (let [_ (println :read-data)
        data (m/add-ids (m/read-data))
        _ (println :divide-corpus)
        g (c/divide-corpus (map :description data) "hoax")
        _ (println :disj-hoax)
        groups (assoc g true (map #(disj % "hoax") (get g true)))
        _ (println :group-keys)
        group-keys (keys groups)
        k 300
        _ (println :create-sets)
        [test-set training-set] (create-sets groups k)
        _ (println :bayes)
        bayes (-> (b/new-bayes [:hoax :non-hoax])
                (b/train-all :hoax (get training-set true))
                (b/train-all :non-hoax (get training-set false))
                b/finish-training)]
    (assoc system
           :data data
           :groups groups
           :group-keys group-keys
           :test-set test-set
           :bayes bayes)))

(defn stop [system]
  system)

