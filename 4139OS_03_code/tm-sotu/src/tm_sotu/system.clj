
(ns tm-sotu.system
  (:require [net.cgrand.enlive-html :as en]
            [tm-sotu.download :as dl]
            [tm-sotu.topic-model :as tm])
  (:import [java.net URL]))

(defn download-base [s]
  (println :download-base)
  (assoc s :index (en/html-resource (URL. (:index-url s)))))

(defn download-page [s]
  (println :download-page)
  (let [links (dl/get-index-links (:index s))
        page (-> links first :attrs :href URL. en/html-resource)]
    (assoc s
           :links links
           :page page)))

(defn system []
  {:index-url "http://www.presidency.ucsb.edu/sou.php"
   :data-dir "www/data"})

(defn start
  [system]
  (let [instances (tm/make-pipe-list)]
    (tm/add-directory-files instances (:data-dir system))
    (assoc system
           :instances instances
           :model (tm/train-model instances))))

(defn stop
  [system]
  system)

