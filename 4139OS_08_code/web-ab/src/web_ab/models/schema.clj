(ns web-ab.models.schema
  (:require [clojure.java.jdbc :as sql]
            [noir.io :as io]))

(def db-store "site.db")

(def db-spec {:classname "org.h2.Driver"
              :subprotocol "h2"
              :subname (str (io/resource-path) db-store)
              :user "sa"
              :password ""
              :naming {:keys clojure.string/lower-case
                       :fields clojure.string/upper-case}})
(defn initialized?
  "checks to see if the database schema is present"
  []
  (.exists (new java.io.File (str (io/resource-path) db-store ".h2.db"))))

(defn create-abtracking-table []
  (sql/with-connection db-spec
    (sql/create-table :abtracking
      [:id "INTEGER IDENTITY"]
      [:testgroup "INT NOT NULL"]
      [:startat "TIMESTAMP NOT NULL DEFAULT NOW()"]
      [:succeed "TIMESTAMP DEFAULT NULL"])))

(defn create-tables
  "creates the database tables used by the application"
  []
  (create-abtracking-table))
