(ns social-so.post
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [social-so.data :as d]
            [social-so.utils :as u]
            [social-so.xml :as x]))

(defrecord PostInfo
  [id post-type body-text score accepted-for])

(defn save-post-infos [filename posts]
  (with-open [w (io/writer filename)]
    (json/write posts w)))

(defn load-post-infos [filename]
  (with-open [r (io/reader filename)]
    (doall
      (->> (json/read r :key-fn keyword)
        (map map->PostInfo)
        (map #(assoc % :post-type (keyword (:post-type %))))))))

(defn el->post-info [el]
  (->PostInfo (x/get-id el)
              (case (x/get-post-type el)
                1 :q
                2 :a
                :unknown)
              (x/get-body-text el)
              (x/get-score el)
              nil))

(defn read-post-infos [stream]
  (map el->post-info (x/read-posts stream)))

(defn mark-accepted [accepted post]
  (assoc post :accepted-for (accepted (:id post))))

(defn mark-accepted-posts [accepted posts]
  (map #(mark-accepted accepted %) posts))

(defn pull-accepted [index el]
  (if-let [accepted-id (x/get-accepted-id el)]
    (if-let [indexed (index accepted-id)]
      (assoc-in index
                [accepted-id :accepted-for]
                (x/get-id el))
      index)
    index))

(defn index-posts [coll]
  (into {} (map #(vector (:id %) %) coll)))

(defn read-sample-post-infos [sample-file full-file]
  (println "reading samples")
  (let [sample (with-open [s-in (io/input-stream sample-file)]
                 (doall
                   (index-posts (read-post-infos s-in))))]
    (println "reading accepted ids")
    (with-open [s-in (io/input-stream full-file)]
      (vals
        (reduce pull-accepted sample (x/read-posts s-in))))))

