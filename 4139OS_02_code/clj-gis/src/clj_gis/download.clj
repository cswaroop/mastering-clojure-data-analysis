
(ns clj-gis.download
  (:require [clojure.java.io :as io]
            [me.raynes.fs.compression :as compression]
            [me.raynes.fs :as fs]
            [miner.ftp :as ftp]
            [clj-gis.locations :as loc]
            [clj-gis.util :as u])
  (:import [org.apache.commons.net.ftp FTP]
           [java.util.zip GZIPInputStream]
           [java.io BufferedInputStream]))

(def connection-uri
  "ftp://anonymous:erochest%40gmail.com@ftp.ncdc.noaa.gov/pub/data/gsod/")

(defn get-year
  [path]
  (re-find #"\d{4}" path))

(defn download-src
  [dirname]
  (str dirname "/gsod_" dirname ".tar"))

(defn download-dest
  ([dirname] (download-dest loc/*download-dir* dirname))
  ([data-dir dirname]
   (fs/file data-dir (str "gsod_" dirname ".tar"))))

(defn download-file
  "Download a single file from FTP into a download directory."
  ([client dirname]
   (download-file client loc/*download-dir* dirname))
  ([client download-dir dirname]
   (let [src (download-src dirname)
         dest (download-dest download-dir dirname)]
     (u/log :download-file src :=> dest)
     (ftp/client-get client src dest))))

(defn download-data
  "Connect to an FTP server and download the GSOD data files."
  ([] (download-data connection-uri loc/*download-dir* loc/*data-dir*))
  ([uri download-dir data-dir]
   (let [download-dir (io/file download-dir)
         data-dir (io/file data-dir)]
     (u/ensure-dir download-dir)
     (u/ensure-dir data-dir)
     (ftp/with-ftp [client uri]
       (.setFileType client FTP/BINARY_FILE_TYPE)
       (doseq [dirname (filter get-year (ftp/client-directory-names client))]
         (download-file client download-dir dirname))))))

(defn untar
  "Untar the file into the destination directory."
  [input-file dest-dir]
  (u/log :untar input-file :=> dest-dir)
  (compression/untar input-file dest-dir))

(defn gunzip
  "Gunzip the input file and delete the original."
  [input-file]
  (let [input-file (fs/file input-file)
        parts (fs/split input-file)
        dest (fs/file (reduce fs/file (butlast parts))
                      (first (fs/split-ext (last parts))))]
    (u/log :gunzip input-file :=> dest)
    (with-open [f-in (BufferedInputStream.
                       (GZIPInputStream.
                         (io/input-stream input-file)))]
      (with-open [f-out (io/output-stream dest)]
        (io/copy f-in f-out)))))

(defn download-all
  []
  (let [tar-dir (fs/file loc/*download-dir*)
        data-dir (fs/file loc/*data-dir*)]
    (download-data tar-dir data-dir)
    (doseq [tar-file (fs/list-dir tar-dir)]
      (untar (fs/file tar-dir tar-file) data-dir))
    (doseq [gz-file (fs/list-dir data-dir)]
      (gunzip (fs/file data-dir gz-file)))))

