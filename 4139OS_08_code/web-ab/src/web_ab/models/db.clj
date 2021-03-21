(ns web-ab.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [web-ab.models.schema :as schema]
            [taoensso.timbre :as timbre]))

(defdb db schema/db-spec)

(defentity abtracking)

(def group-code {:control 0, :test 1})
(def code->group (zipmap (vals group-code) (keys group-code)))

(defn clear-abtracking []
  (delete abtracking))

(defn create-abtracking [group]
  (get (insert abtracking (values [{:testgroup (group-code group)}]))
       (keyword "scope_identity()")))

(defn get-abtracking [id]
  (timbre/info :get-abtracking id)
  (first (select abtracking
                 (where {:id id})
                 (limit 1))))

(defn get-all-abtracking []
  (select abtracking
          (order :startat :desc)))

(defn mark-succeed [id]
  (update abtracking
          (set-fields {:succeed (sqlfn :now)})
          (where {:id id})))
