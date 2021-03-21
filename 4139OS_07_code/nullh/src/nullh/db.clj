
(ns nullh.db
  (:require [incanter.mongodb :as im]
            [somnium.congomongo :as m]
            [nullh.geo :as geo]
            [nullh.ucr :as ucr]))

;; read files
;; pull out agency set and load into db
;; pull out data, key to agencies, and load into db

(defn make-agency-id [agency-map]
  (let [{:keys [agency state]} agency-map]
    (str agency ", " state)))

(defn key-agency [agency]
  (assoc agency :agency_name (make-agency-id agency)))

(defn link-agency [row]
  (dissoc row :agency :state))

(defn select-agencies [data]
  (set (map #(select-keys % [:agency :state :agency_name]) data)))

(defn load-ucr-data [data]
  (let [keyed (map key-agency data)
        agencies (select-agencies keyed)
        geo-data (into {} (geo/geocode-all (map :agency_name agencies)))
        agencies-geo (map #(assoc %1 :location %2)
                          agencies
                          (map geo-data
                               (map :agency_name agencies)))
        ucr-data (map link-agency keyed)]
    (println "Inserting agencies.")
    (dorun (m/mass-insert! :agencies agencies-geo))
    (println "Inserting crime data.")
    (dorun (m/mass-insert! :ucr_data ucr-data))
    (println "done.")))

(comment

  (m/with-mongo (:cxn system) (m/drop-database! "nullh"))
  (m/with-mongo (:cxn system) (db/load-ucr-data (:data system)))

  (m/with-mongo (:cxn system) (m/add-index! :agencies [[:location :2dsphere]]))

  )
