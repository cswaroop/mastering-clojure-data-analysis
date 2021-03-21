(ns financial.oanc
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [me.raynes.fs :as fs]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format])
  (:use [financial types utils]))

(def ^:dynamic *oanc-dir* (io/file "d/OANC-GrAF"))
(def date-time-format (time-format/formatter "M/d/yyyy h:mm:ss a"))

(defn list-category-genres [category-dir]
  (map #(hash-map :genre % :dirname (io/file category-dir %))
       (fs/list-dir category-dir)))

(defn list-genres [oanc-dir]
  (mapcat list-category-genres (ls (io/file oanc-dir "data"))))

(defn find-genre-dir [genre oanc-dir]
  (->> oanc-dir
    list-genres
    (filter #(= (:genre %) genre))
    first
    :dirname))

(defn find-source-data [genre source oanc-dir]
  (-> (find-genre-dir genre oanc-dir)
    (io/file source)
    (fs/find-files #".*\.anc")))

(defn find-slate-files [oanc-dir]
  (map #(hash-map :anc % :txt (chext % ".txt"))
       (find-source-data "journal" "slate" oanc-dir)))

(defn find-all [xml tag-name]
  (lazy-seq
    (if (= (:tag xml) tag-name)
      (cons xml (mapcat #(find-all % tag-name) (:content xml)))
      (mapcat #(find-all % tag-name) (:content xml)))))

(defn content-str [xml]
  (apply str (filter string? (:content xml))))

(defn parse-pub-date [pub-date-el]
  (time-format/parse date-time-format (content-str pub-date-el)))

(defn norm-date [date]
  (cond
    (= (clj-time/year date) 0) (clj-time/plus date (clj-time/years 2000))
    (< (clj-time/year date) 100) (clj-time/plus date (clj-time/years 1900))
    :else date))

(defn find-pub-date [anc-xml]
  (-> anc-xml
    (find-all :pubDate)
    first
    parse-pub-date
    norm-date))

(defn find-title [anc-xml]
  (content-str (first (find-all anc-xml :title))))

(defn load-article [data-info]
  (let [{:keys [anc txt]} data-info
        anc-xml (xml/parse (io/reader anc))]
    (->NewsArticle (find-title anc-xml)
                   (find-pub-date anc-xml)
                   (slurp txt))))

(defn load-text-file [date filename]
  (->NewsArticle filename date (slurp filename)))
