(ns web-ab.routes.home
  (:use compojure.core)
  (:require [web-ab.views.layout :as layout]
            [web-ab.util :as util]
            [web-ab.models.db :as db]
            [web-ab.ab-testing :as ab]
            [noir.cookies :as c]))

(def default-button "Learn more!")
(def test-cases {:control default-button, :test "Order now!"})

(defn home-page []
  (layout/render
    "home.html"
    {:button (ab/start-test :visits default-button test-cases)}))

(defn purchase-page []
  (ab/mark-succeed)
  (layout/render "purchase.html" {}))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/purchase/" [] (purchase-page)))
