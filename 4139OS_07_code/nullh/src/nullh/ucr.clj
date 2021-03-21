
(ns nullh.ucr
  (:require [net.cgrand.enlive-html :as html]
            [http.async.client :as http]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [me.raynes.fs :as fs])
  (:import [java.net URL]
           [java.util Date]
           [java.text SimpleDateFormat]
           [java.lang StringBuffer]
           [java.io StringReader]))

(def ^:dynamic *base-ucr-uri*
  (URL. "http://www.ucrdatatool.gov/Search/Crime/Local/"))

;;;; http://www.ucrdatatool.gov/Search/Crime/Local/OneYearofData.cfm
;;; form#CFForm_1 => action = POST OneYearofDataStepTwo.cfm 
;; select#state  => StateId = 1-51
;; select#groups => BJSPopulationGroupId = (empty)|2-19
;; NextPage = Next
;; DataType = 0

(defn select-values [html select-id]
  (->> (html/select html [select-id :option])
    (map :attrs)
    (map :value)))

(defn load-states
  ([] (load-states (URL. *base-ucr-uri* "OneYearofData.cfm")))
  ([uri]
   (with-open [client (http/create-client)]
     (load-states client uri)))
  ([client uri]
   (println :load-states)
   (let [html (->> uri
                str
                (http/GET client)
                http/await
                http/string
                StringReader.
                html/html-resource)]
     {:StateId (select-values html :#state)
      :BJSPopulationGroupId "" #_(select-values html :#groups)})))

;;;; http://www.ucrdatatool.gov/Search/Crime/Local/OneYearofDataStepTwo.cfm
;;; form#CFForm_1 => action = POST RunCrimeOneYearofData.cfm
;; StateId, BJSPopulationGroupId
;; select#agencies => CrimeCrossId=./option/@value (many)
;; select#groups   => DataType=1-4                 (many)
;; select#year     => YearStart=./option/@value    (one)
;; NextPage = Get Table

(defn spread-states [state-resp]
  (map #(vector % "") (:StateId state-resp)))

(defn load-agencies
  ([state-id group-id]
   (load-agencies state-id group-id (URL. *base-ucr-uri* "OneYearofDataStepTwo.cfm")))
  ([state-id group-id uri]
   (with-open [client (http/create-client)]
     (load-agencies state-id group-id client uri)))
  ([state-id group-id client uri]
   (println :load-agencies state-id group-id)
   (let [params {:StateId state-id,
                 :BJSPopulationGroupId group-id,
                 :NextPage "Next"
                 :DataType 0}
         html (->> (http/POST client (str uri) :body params)
                http/await
                http/string
                StringReader.
                html/html-resource)]
     {:CrimeCrossId (select-values html :#agencies)
      :DataType (select-values html :#groups)
      :YearStart (select-values html :#year)})))

;;;; POST DownCrimeOneYearofData.cfm/LocalCrimeOneYearofData.csv
;;; CrimeCrossId = 102,104,105,110
;;; YearStart    = 1985
;;; YearEnd      = 1985
;;; DataType     = 1,2,3,4

(defn spread-agencies [agency-resp]
  (map #(assoc agency-resp :YearStart %) (:YearStart agency-resp)))

(defn load-csv
  ([cross-ids data-types year]
   (load-csv cross-ids data-types year
             (URL. *base-ucr-uri* "DownCrimeOneYearofData.cfm/LocalCrimeOneYearofData.csv")))
  ([cross-ids data-types year uri]
   (with-open [client (http/create-client)]
     (load-csv cross-ids data-types year client uri)))
  ([cross-ids data-types year client uri]
   (println :load-csv year)
   (let [params {:CrimeCrossId (str/join \, cross-ids)
                 :YearStart year
                 :YearEnd year
                 :DataType (str/join \, data-types)}]
     (->> (http/POST client (str uri) :body params)
       http/await
       http/string))))

(defn save-csv [output-dir data]
  (let [df (SimpleDateFormat. "yyyyMMdd-HHmmss.SSS")
        output-name (str output-dir \/
                         (.format df (Date.))
                         ".csv")]
    (println :save-csv output-name)
    (with-open [w (io/writer output-name)]
      (.write w data))))

;;;; The controller function

(defn download-all [output-dir]
  (->> (load-states)
    spread-states
    (map #(load-agencies (first %) (second %)))
    (mapcat spread-agencies)
    (map #(load-csv (:CrimeCrossId %) (:DataType %) (:YearStart %)))
    (remove nil?)
    (map #(save-csv output-dir %))))

;;;; Let's read all the data.

(defn ->fn [f x default]
  (if (str/blank? x)
    default
    (try
      (f (str/trim x))
      (catch Exception _
        x))))

(defn ->int [x] (->fn #(Long/parseLong %) x 0))
(defn ->double [x] (->fn #(Double/parseDouble %) x 0.0))

(def ucr-coercions
  {:months ->int
   :population ->int
   :violent_total ->int
   :murder_total ->int
   :rape_total ->double
   :robbery_total ->double
   :assault_total ->double
   :property_total ->double
   :burglary_total ->double
   :larceny_total ->double
   :motor_total ->double
   :violent_rate ->double
   :murder_rate ->double
   :rape_rate ->double
   :robbery_rate ->double
   :assault_rate ->double
   :property_rate ->double
   :burglary_rate ->double
   :larceny_rate ->double
   :motor_rate ->double})

(def ucr-keys
  {"Agency" :agency
   "State" :state
   "Months" :months
   "Population" :population
   "Violent crime total" :violent_total
   "Murder and nonnegligent Manslaughter" :murder_total
   "Forcible rape" :rape_total
   "Robbery" :robbery_total
   "Aggravated assault" :assault_total
   "Property crime total" :property_total
   "Burglary" :burglary_total
   "Larceny-theft" :larceny_total
   "Motor vehicle theft" :motor_total
   "Violent Crime rate" :violent_rate
   "Murder and nonnegligent manslaughter rate" :murder_rate
   "Forcible rape rate" :rape_rate
   "Robbery rate" :robbery_rate
   "Aggravated assault rate" :assault_rate
   "Property crime rate" :property_rate
   "Burglary rate" :burglary_rate
   "Larceny-theft rate" :larceny_rate
   "Motor vehicle theft rate" :motor_rate})

(defn coerce
  ([k v] (coerce ucr-coercions k v))
  ([coercions k v] [k ((coercions k identity) v)]))

(defn read-header [lines]
  (let [[year-line _ & lines] (drop 3 lines)]
    [{:year (->int (re-find #"\d{4}" year-line))}, lines]))

(defn drop-footer [lines]
  (let [data-line (fn [line]
                    (not (empty? (str/trim line))))]
    [nil, (take-while data-line lines)]))

(def nl "\r\n")

(defn process-lines [reader fns]
  (let [proc (fn [[lines out] f]
               (let [[data lines] (f lines)]
                 [lines (conj out data)]))
        [lines output] (reduce proc [(line-seq reader) []] fns)
        buffer (reduce #(.. %1 (append %2) (append nl)) (StringBuffer.) lines)]
    [(io/reader (StringReader. (str buffer))) output]))

(defn mapmap [f collcoll]
  (let [inner (fn [coll] (map f coll))]
    (map inner collcoll)))

(defn read-ucr [filename]
  (with-open [r (io/reader filename)]
    (let [[r [data-seed _]] (process-lines r [read-header drop-footer])
          [header & data] (csv/read-csv r)
          header (map #(ucr-keys % %) header)]
      (doall
        (->> data
          (map #(zipmap header %))
          (mapmap #(coerce (first %) (second %)))
          (map #(into data-seed %)))))))

(defn read-all-ucr [data-dir]
  (->> data-dir
    fs/list-dir
    (map #(str data-dir \/ %))
    (mapcat read-ucr)))


