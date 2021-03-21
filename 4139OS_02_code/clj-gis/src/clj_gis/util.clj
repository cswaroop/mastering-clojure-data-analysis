
(ns clj-gis.util
  (:require [clojure.string :as str])
  (:import [java.io File]))

(defn log
  [src & args]
  (print (str (str/join " " (concat [\[ (str (java.util.Date.)) \] src]
                                    (map str args)))
              "\n"))
  (flush))

(defn log-every
  [n m src & args]
  (when (zero? (mod n m))
    (apply log src args)))

(defn ensure-dir
  [^File dir]
  (when-not (.exists dir)
    (.mkdir dir)))

(defn group-seq
  "This is a lazy version of group-by. Instead of returning a map, it returns a
  seq of [key [value]] pairs."
  [key-fn coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [k (key-fn (first s))
            [head-xs tail-xs] (split-with #(= k (key-fn %)) s)]
        (cons [k head-xs] (group-seq key-fn tail-xs))))))

(defn ->float [x] (Double/parseDouble x))

