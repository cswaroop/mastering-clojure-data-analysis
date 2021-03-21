
(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [net.cgrand.enlive-html :as en]
            [tm-sotu.system :as system]
            [tm-sotu.download :as dl]
            [tm-sotu.topic-model :as tm])
  (:import [java.net URL]
           [java.io File]))

(def system nil)

(defn init
  []
  (alter-var-root #'system (constantly (system/system))))

(defn start
  []
  (alter-var-root #'system system/start))

(defn stop
  []
  (alter-var-root #'system (fn [s] (when s (system/stop s)))))

(defn go
  []
  (init)
  (start)
  nil)

(defn reset
  []
  (stop)
  (refresh :after 'user/go))

(defn update [f]
  (alter-var-root #'system f)
  nil)

(defn rerun [topics]
  (update (fn [s]
            (assoc s
                   :model (tm/generate-dump-model (:instances s) topics 100 "www/topic"))))
  (let [counts (tm/topic-counts (:model system) (:instances system))]
    (pprint (reverse (sort-by second counts)))
    counts))


(defn topic-words [topic n]
  (pprint
    (take n
          (tm/get-topic-words (:model system) (:instances system) topic))))

