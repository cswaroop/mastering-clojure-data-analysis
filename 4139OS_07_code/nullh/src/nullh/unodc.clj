(ns nullh.unodc
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [me.raynes.fs :as fs]
            [clojure.data.json :as json]
            [nullh.utils :as u])
  (:import
    [java.io FileInputStream]
    [org.apache.poi.ss.usermodel
     Cell CellStyle DataFormat Font RichTextString Row Sheet]
    [org.apache.poi.hssf.usermodel HSSFWorkbook]))

;; The input datatype
(defrecord xl-row
  [sheet region sub-region country
   count-2003 count-2004 count-2005 count-2006 count-2007 count-2008
   count-2009 count-2010 count-2011
   rate-2003 rate-2004 rate-2005 rate-2006 rate-2007 rate-2008
   rate-2009 rate-2010 rate-2011])

(defn seq->xl-row [coll] (apply ->xl-row coll))

;; Gets converted into this tree-like structure.
(defrecord region [region-name sub-regions])
(defrecord sub-region [sub-region-name countries])
(defrecord country [country-name counts rates sheet])
(defrecord yearly-data
   [year-2003 year-2004 year-2005 year-2006 year-2007 year-2008
    year-2009 year-2010 year-2011])

(defn xl-row->yearly [coll fields]
  (apply ->yearly-data (map #(get coll %) fields)))

(defn xl-row->counts [coll]
  (xl-row->yearly
    coll
    [:count-2003 :count-2004 :count-2005 :count-2006 :count-2007
     :count-2008 :count-2009 :count-2010 :count-2011]))

(defn xl-row->rates [coll]
  (xl-row->yearly
    coll
    [:rate-2003 :rate-2004 :rate-2005 :rate-2006 :rate-2007
     :rate-2008 :rate-2009 :rate-2010 :rate-2011]))

(defn xl-rows->countries [coll]
  (->> coll
    (group-by :country)
    (map #(let [[country-name [row & _]] %]
            (->country country-name
                       (xl-row->counts row)
                       (xl-row->rates row)
                       (:sheet row))))))

(defn conj-into [m k v]
  (if (contains? m k)
    (assoc m k (conj (get m k) v))
    (assoc m k [v])))

(defn fold-sub-region [state row]
  (let [[current accum] state]
    (if (str/blank? (:sub-region row))
      [current
       (conj-into accum current (assoc row :sub-region current))]
      (let [new-sub-region (:sub-region row)]
        [new-sub-region
         (conj-into accum new-sub-region row)]))))

(defn xl-rows->sub-regions [coll]
  (->> coll
    (reduce fold-sub-region [nil {}])
    second
    (map #(->sub-region
            (first %) (xl-rows->countries (second %))))))

(defn xl-rows->regions [coll]
  (->> coll
    (group-by :region)
    (map #(->region
            (first %) (xl-rows->sub-regions (second %))))))

(defn country->xl-rows [tree sub-region-row]
  (let [counts (:counts tree), rates (:rates tree)]
    (assoc sub-region-row
           :sheet (:sheet tree)
           :country (:country-name tree)
           :count-2003 (:year-2003 counts)
           :count-2004 (:year-2004 counts)
           :count-2005 (:year-2005 counts)
           :count-2006 (:year-2006 counts)
           :count-2007 (:year-2007 counts)
           :count-2008 (:year-2008 counts)
           :count-2009 (:year-2009 counts)
           :count-2010 (:year-2010 counts)
           :count-2011 (:year-2011 counts)
           :rate-2003 (:year-2003 rates)
           :rate-2004 (:year-2004 rates)
           :rate-2005 (:year-2005 rates)
           :rate-2006 (:year-2006 rates)
           :rate-2007 (:year-2007 rates)
           :rate-2008 (:year-2008 rates)
           :rate-2009 (:year-2009 rates)
           :rate-2010 (:year-2010 rates)
           :rate-2011 (:year-2011 rates))))

(defn sub-regions->xl-rows [tree region-row]
  (let [sub-region-row (assoc region-row :sub-region
                              (:sub-region-name tree))]
    (map #(country->xl-rows % sub-region-row) (:countries tree))))

(defn region->xl-rows [tree nil-row]
  (let [region-row (assoc nil-row :region (:region-name tree))]
    (mapcat #(sub-regions->xl-rows % region-row)
            (:sub-regions tree))))

(defn regions->xl-rows [region-coll]
  (let [nil-row (seq->xl-row (repeat 22 nil))]
    (mapcat #(region->xl-rows % nil-row) region-coll)))

(defn open-file [filename]
  (with-open [s (io/input-stream filename)]
    (HSSFWorkbook. s)))

(defn sheets [workbook]
  (->> workbook
    (.getNumberOfSheets)
    (range)
    (map #(.getSheetAt workbook %))))

(defn rows [sheet]
  (->> sheet
    (.getPhysicalNumberOfRows)
    (range)
    (map #(.getRow sheet %))
    (remove nil?)))

(defn cells [row]
  (->> row
    (.getPhysicalNumberOfCells)
    (range)
    (map #(.getCell row %))))

(defn cell-type [cell]
  (if (nil? cell)
    nil
    (let [cell-types {Cell/CELL_TYPE_BLANK   :blank
                      Cell/CELL_TYPE_BOOLEAN :boolean
                      Cell/CELL_TYPE_ERROR   :error
                      Cell/CELL_TYPE_FORMULA :formula
                      Cell/CELL_TYPE_NUMERIC :numeric
                      Cell/CELL_TYPE_STRING  :string}]
      (cell-types (.getCellType cell)))))

(defmulti cell-value cell-type)

(defmethod cell-value :blank   [_] nil)
(defmethod cell-value :boolean [c] (.getBooleanCellValue c))
(defmethod cell-value :error   [c] (.getErrorCellValue   c))
(defmethod cell-value :formula [c] (.getErrorCellValue   c))
(defmethod cell-value :numeric [c] (.getNumericCellValue c))
(defmethod cell-value :string  [c] (.getStringCellValue  c))
(defmethod cell-value :default [c] nil)

(defn row-values [sheet-name row]
  (conj (mapv cell-value (cells row)) sheet-name))

(defn sheet-data->seq [sheet]
  (map #(row-values (:sheet-name sheet) %) (:sheet-rows sheet)))

(defrecord sheet-data [sheet-name sheet-rows])

(defn on-rows [sheet f]
  (assoc sheet :sheet-rows (f (:sheet-rows sheet))))

(defn get-sheet-data [sheet]
  (->sheet-data (.getSheetName sheet) (rows sheet)))

(defn first-cell-empty? [cells]
  (empty? (cell-value (first cells))))

(defn skip-headers [sheet]
  (on-rows sheet (fn [r]
                   (->> r
                     (drop-while #(first-cell-empty? (cells %)))
                     (drop 1)
                     (take-while #(not (first-cell-empty? %)))))))

(defn clean-row [row]
  (u/pad-vec 22
             (concat (list (last row) (first row))
                     (take 11 (drop 3 row))
                     (drop 15 row))))

(defn read-sheets [filename]
  (->> filename
    (open-file)
    (sheets)
    (map get-sheet-data)
    (map skip-headers)
    (map (fn [s] (on-rows s #(remove empty? %))))
    (mapcat sheet-data->seq)
    (map clean-row)
    (map seq->xl-row)
    (xl-rows->regions)
    (regions->xl-rows)))

(defn convert-sheets [input output]
  (println input \tab output)
  (with-open [out (io/writer output)]
    (json/write (read-sheets input) out)))

(defn convert-dir [in-dir out-dir]
  (doseq [in-file (u/ls in-dir)]
    (let [[_ filename] (fs/split in-file)
          [basename _] (fs/split-ext filename)]
      (convert-sheets in-file
                      (str out-dir \/ basename ".json")))))

(comment

  (reset)

  (def filename "unodc-data/CTS_Assault.xls")
  (def f (un/open-file filename))
  (def s (un/sheets f))
  (def sd (map (fn [s] (un/on-rows s #(remove empty? %))) (map un/skip-headers (map un/get-sheet-data s))))
  (:sheet-name (first sd))
  (count (:sheet-rows (first sd)))
  (def sds (mapcat un/sheet-data->seq sd))
  (first sds)
  (def sdc (map un/clean-row sds))
  (first sdc)
  (def xlr (map un/seq->xl-row sdc))
  (first xlr)

  (def assault (un/read-sheets filename))
  (keys (first assault))
  (vals (first assault))
  (first assault)
  (:sheet (first assault))
  (set (map :sheet assault))

  )

