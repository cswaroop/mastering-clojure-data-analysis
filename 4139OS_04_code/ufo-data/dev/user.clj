
(ns user
  (:require [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.set :as set]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [ufo-data.analysis :as a]
            [ufo-data.bayes :as b]
            [ufo-data.corpus :as c]
            [ufo-data.model :as m]
            [ufo-data.system :as system]
            [ufo-data.text :as t]
            [ufo-data.tm :as tm]
            [ufo-data.util :as u]
            [clj-time.core :as time]
            [incanter.core :as i]
            [incanter.io :as iio]
            [incanter.stats :as s]
            #_[me.raynes.fs :as fs]))

(def ^:dynamic *top-term-count* 75)

(def system nil)

(defn init
  []
  (alter-var-root #'system
                  (constantly (system/system))))

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

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn update [f]
  (alter-var-root #'system f)
  nil)

(defn view-top [key n]
  (pprint (take n (key system))))

(defn update-top [update-f key n]
  (update-f)
  (view-top key n))

(defn ensure-or [key update-f]
  (if (contains? system key)
    system
    (do
      (update-f)
      system)))

(defn add-key [k f d]
  (assoc d k (f d)))

(defn on-second [f]
  (fn [[a b]] (vector a (f b))))

(defn update-key [k f]
  (println k)
  (update (fn [s] (assoc s k (f s)))))

(defn update-date-shape []
  (update-key :date-shape #(a/get-all-date-shapes (:data %))))

(defn update-shape-freqs []
  (update-key :shape-freqs #(a/get-shape-freqs (:data %) 2)))

(defn write-term-freqs []
  (ensure-or :shape-freqs update-shape-freqs)
  (let [fqs (:shape-freqs system)]
    (println :write-term-freqs)
    (with-open [f (io/writer "www/term-freqs.json")]
      (json/write fqs f))))

(defn update-by-year []
  (ensure-or :date-shape update-date-shape)
  (update-key :by-year #(a/group-by-year (:date-shape %))))

(defn update-year-counts []
  (ensure-or :by-year update-by-year)
  (update-key :year-counts #(a/get-year-counts (:by-year %))))

(defn update-year-freqs []
  (ensure-or :by-year update-by-year)
  (update-key :year-freqs #(->> %
                             :by-year
                             (filter (fn [x] (< 2 (count (second x)))))
                             (map (on-second (fn [x] (frequencies (map :term x))))))))

(defn update-term-set []
  (ensure-or :year-freqs update-year-freqs)
  (update-key :term-set #(set/difference (set (mapcat keys (map second (:year-freqs %))))
                                         t/*stop-list*)))

(defn update-raw-term-freqs []
  (update-key :raw-term-freqs #(->> %
                                 :data
                                 (mapcat a/get-shape-bag)
                                 frequencies)))

(defn update-top-terms
  ([] (update-top-terms *top-term-count*))
  ([n]
   (ensure-or :raw-term-freqs update-raw-term-freqs)
   (update-key :top-terms #(->> %
                             :raw-term-freqs
                             (sort-by second)
                             reverse
                             (take n)
                             (map first)))))

(defn write-year-freqs []
  (ensure-or :year-freqs update-year-freqs)
  (ensure-or :top-terms update-top-terms)
  (let [yf (:year-freqs system),
        ts (vec (:top-terms system))]
    (println :write-year-freqs)
    (with-open [f (io/writer "www/year-freqs.json")]
      (json/write {:terms ts, :freqs yf} f))))

(defn update-year-hist []
  (ensure-or :by-year update-by-year)
  (println :update-year-hist)
  (update-key :year-hist #(a/get-year-counts (a/group-by-year (:data %)))))

(defn write-year-hist []
  (let [yh (:year-hist system)]
    (println :write-year-hist)
    (with-open [f (io/writer "www/year-hist.json")]
      (json/write yh f))))

(defn update-by-month []
  (println :update-by-month)
  (update-key :by-month #(a/group-by-month (:data %))))

(defn write-month-hist []
  (ensure-or :by-month update-by-month)
  (let [bm (:by-month system)]
    (println :write-month-hist)
    (with-open [f (io/writer "www/month-hist.json")]
      (json/write bm f))))

(defn write-descr-counts
  ([] (write-descr-counts (a/get-descriptions (:data system))))
  ([descr-counts]
   (with-open [f (io/writer "www/descr-freqs.json")]
     (json/write descr-counts f))))

(defn add-year-month []
  (let [seasons {1 1, 2 1, 3 2, 4 2, 5 2, 6 3,
                 7 3, 8 3, 9 4, 10 4, 11 4, 12 1}]
    (update-key :data
                (fn [s]
                  (->> s
                    :data
                    (filter :sighted-at)
                    (map (fn [d]
                           (let [sighted-at (:sighted-at d)
                                 month (time/month sighted-at)]
                             (assoc d
                                    :year (time/year sighted-at)
                                    :month month
                                    :season (seasons month))))))))))

(defn update-ym-groups []
  (update-key :ym-groups #(group-by (fn [d]
                                      [(:year d) (:month d) (:season d)])
                                    (:data %))))

(defn update-ym-counts []
  (ensure-or :ym-groups update-ym-groups)
  (update-key :ym-counts
              #(->> %
                 :ym-groups
                 (map (on-second count))
                 (map flatten)
                 (map (partial zipmap [:year :month :season :count])))))

(defn update-all-json []
  (write-term-freqs)
  (write-year-freqs)
  (write-year-hist)
  (write-month-hist))

(defn update-ilist []
  (println :ilist)
  (update (fn [s]
            (->> s
              :data
              (map :description)
              (map t/replace-entities)
              (tm/add-strings (tm/make-pipe-list))
              (assoc s :ilist)))))

(defn update-train [num-topics]
  (ensure-or :ilist update-ilist)
  (println :train)
  (update
    (fn [s]
      (->> s
        :ilist
        (tm/train-model num-topics 4 50)
        (assoc s :model)))))

