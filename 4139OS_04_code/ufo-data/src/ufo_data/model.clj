
(ns ufo-data.model
  (:require [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clj-time.format :as tf]
            [ufo-data.text :as t]
            [ufo-data.util :refer :all]
            [me.raynes.fs :as fs])
  (:import [java.lang StringBuffer]))

(def ^:dynamic *data-file* "data/ufo.json")

(defrecord UfoSighting
  [sighted-at reported-at location shape duration description
   year month season])

(def date-formatter (tf/formatter "yyyyMMdd"))

(defn read-date [date-str]
  (try
    (tf/parse date-formatter date-str)
    (catch Exception ex
      nil)))

(defn add-ids [coll]
  (map #(assoc %1 :id %2) coll (range)))

(defn coerce-fields [ufo]
  (assoc ufo
         :sighted-at (read-date (:sighted-at ufo))
         :reported-at (read-date (:reported-at ufo))))

(defn read-data
  ([] (read-data *data-file*))
  ([filename]
   (with-open [f (io/reader filename)]
     (->> f
       line-seq
       vec
       (r/map #(json/read-str % :key-fn keyword))
       (r/map map->UfoSighting)
       (r/map coerce-fields)
       (into [])))))

(defn split-nuforc [text]
  (let [m (.matcher #"\(\(.*?\)\)" text), sb (StringBuffer.)]
    (loop [accum []]
      (if (.find m)
        (let [nuforc (.substring text (.start m) (.end m))]
          (.appendReplacement m sb "")
          (recur (conj accum nuforc)))
        (do
          (.appendTail m sb)
          [(str sb) (str/join " " accum)])))))

(defn get-category [tokens]
  (if (contains? (set tokens) "hoax")
    :hoax
    :non-hoax))

(defn save-document [basedir n doc]
  (let [[text category] doc
        filename (str basedir \/ (name category) \/ n ".txt")]
    (spit filename text)
    {:category category, :filename filename}))

(defn make-dirtree-sighting
  ([basedir]
   (fn [sighting n]
     (make-dirtree-sighting basedir sighting n)))
  ([basedir sighting n]
   (->> sighting
     :description
     split-nuforc
     (on-both #(map t/normalize (t/tokenize %)))
     (on-second get-category)
     (on-first #(str/join " " %))
     (save-document basedir n))))

(defn mk-cat-dirs [base]
  (doseq [cat ["hoax" "non-hoax"]]
    (fs/mkdirs (fs/file base cat))))

(defn into-sets [ratio coll]
  (split-at (int (* (count coll) ratio)) coll))

(defn mv-stage [basedir stage coll]
  (let [stage-dir (fs/file basedir stage)]
    (doseq [{:keys [category filename]} coll]
      (fs/copy filename
               (fs/file stage-dir (name category)
                        (fs/base-name filename))))))

(defn make-dirtree [basedir training-ratio sightings]
  (doseq [dir [basedir (fs/file basedir "train") (fs/file basedir "test")]]
    (mk-cat-dirs dir))
  (let [outputs (map (make-dirtree-sighting basedir) sightings (range))
        {:keys [hoax non-hoax]} (group-by :category (shuffle outputs))
        [hoax-train hoax-test] (into-sets training-ratio hoax)
        [nhoax-train nhoax-test] (into-sets training-ratio non-hoax)]
    (mv-stage basedir "train" (concat hoax-train nhoax-train))
    (mv-stage basedir "test" (concat hoax-test nhoax-test))))
