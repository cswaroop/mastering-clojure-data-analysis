
(ns nullh.xl
  (:require [incanter.core :as i]
            [incanter.excel :as xl]
            [clojure.set :as s]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [me.raynes.fs :as fs])
  (:import [java.util Date]))

(defn status [msg x] (println \[ (str (Date.)) \] msg) x)

(defn get-year [colname]
  "This returns nil if the column has no year in it, or it
  returns the year. This parses column names in the format
  CCCYY.MM (code-year-month)."
  (when-let [m (or (re-matches #"\D*(\d\d)\.\d\d$" colname)
                   (re-matches #"\D*\d?\.(\d\d)$" colname))]
    (let [short-year (Long/parseLong (second m))]
      (if (< short-year 50)
        (+ 2000 short-year)
        (+ 1900 short-year)))))

(defn keep-col? [year col-name]
  (let [col-year (get-year col-name)]
    (or (nil? col-year) (= col-year year))))

(defn filter-year [year m]
  (select-keys m (filter #(keep-col? year %) (keys m))))

(defn sheet->maps
  "This takes a worksheet's columns and its rows of data and
  returns the data as hash-maps."
  ([sheet-dataset]
   (let [cols (i/col-names sheet-dataset)]
     (r/map #(zipmap cols %) (i/to-vect sheet-dataset))))
  ([i sheet-dataset]
   (println :sheet->maps i (type sheet-dataset))
   (sheet->maps sheet-dataset)))

(defn index-by [index-key coll]
  (let [update-conj (fn [m v]
                      (let [k (get v index-key)]
                        (assoc m k (conj (get m k []) v))))
        combine (fn
                  ([] {})
                  ([x y] (merge-with #(vec (concat %1 %2)) x y)))]
    (r/fold combine update-conj coll)))

(defn group-col [colname]
  (or (second (re-matches #"(\D*\d\d)\.\d\d$" colname))
      (second (re-matches #"(\D*\d?\.\d\d)$" colname))
      colname))

(defn reduce-merge [coll] (reduce merge {} coll))

(defn pair-map [f pair]
  (let [[a b] pair]
    [a (f b)]))

(defn mapmap [f collcoll]
  (let [inner (fn [coll] (r/map f coll))]
    (doall
    (map inner collcoll)))
  )

(defn summarize-years [sheet-data]
  (let [sum (fn [coll] (reduce + 0 (remove nil? coll)))
        summarize (fn [vs]
                    (case (count vs)
                      0 0
                      1 (second (first vs))
                      (sum (map second vs))))]
    (->> sheet-data
      (remove #(let [s (second %)]
                 (and (number? s) (<= s 0))))
      (group-by #(group-col (first %)))
      (map #(pair-map summarize %))
      (into {}))))

(defn concat-vals
  ([] {})
  ([a b] (merge-with #(vec (concat %1 %2)) a b)))

(defn read-ucr-xls
  "This reads the data from a UCR Excel file and returns its data
  as a sequence of hash-maps."
  [year xls-filename]
  (println :read-ucr-xls year xls-filename)
  (->>
    (xl/read-xls xls-filename :all-sheets? true)
    butlast
    vec
    (status "\tread")
    (map sheet->maps)
    (mapmap #(filter-year year %))
    (status "\tfiltered")
    (mapmap summarize-years)
    (status "\tsummarized")
    (map #(index-by "ori_code" %))
    (r/fold concat-vals concat-vals)
    (status "\tindexed")
    (map second)
    (map reduce-merge)))

(defn read-ucr-dir
  "This reads the data from a directory of UCR Excel files and
  returns all of their data as a sequence of hash-maps."
  [year dirname]
  (->>
    dirname
    fs/list-dir
    (map #(str dirname \/ %))
    (mapcat #(read-ucr-xls year %))))

(defn write-to-json [data filename]
  (println :write-to-json filename)
  (with-open [f (io/writer filename)]
    (json/write data f)))

(defn ucr-dir->json
  "This reads the directory of UCR Excel files and for each file creates a JSON
  file in another directory."
  [year input-dir output-dir]
  (dorun
    (->>
      input-dir
      fs/list-dir
      (map #(vector (str input-dir \/ %)
                    (str output-dir \/ (fs/base-name % true) ".json")))
      (remove #(fs/exists? (second %)))
      (map #(vector (read-ucr-xls year (first %)) (second %)))
      (map #(write-to-json (first %) (second %))))))

(comment

(reset)
(xl/ucr-dir->json 2004 "all-ucr-data" "json-data")

(def d (xl/read-ucr-dir 2004 "all-ucr-data"))
(with-open [f (io/writer "crime-data.json")] (json/write d f))

  )

