(ns web-ab.routes.ab-testing
  (:use korma.core
        compojure.core)
  (:require [web-ab.views.layout :as layout]
            [web-ab.util :as util]
            [web-ab.models.db :as db]
            [web-ab.ab-testing :as ab]
            [noir.response :as r]
            [taoensso.timbre :as timbre]))

(def target-counts {:control 1200, :test 1200})

(defn is-over?
  ([] (is-over? (ab/get-ab-tracking)))
  ([ab-tracking]
   (let [{:keys [control test]} (ab/group-by-group ab-tracking)]
     (and (>= (count control) (:control target-counts))
          (>= (count test) (:test target-counts))))))

(defn when-is-over [ab-tracking f]
  (if (is-over? ab-tracking)
    (f ab-tracking)
    (let [{:keys [control test]} (ab/group-by-group ab-tracking)]
      (layout/render
        "ab-testing-not-done.html"
        {:counts {:control (count control)
                  :test (count test)}
         :targets target-counts
         :complete {:control (ab/pct (count control)
                                     (:control target-counts))
                    :test (ab/pct (count test)
                                  (:test target-counts))}}))))

(defn index []
  (let [ab-tracking (ab/get-ab-tracking)]
    (layout/render "ab-testing-index.html"
                   {:ab-testing ab-tracking
                    :is-over (is-over? ab-tracking)})))

(defn delete-all []
  (delete db/abtracking)
  (r/json {:response "ok"}))

(defn grid []
  (when-is-over
    (ab/get-ab-tracking)
    (fn [ab-tracking]
      (let [by-group-outcome (ab/assoc-grid-totals
                               (ab/get-results-grid ab-tracking))
            stats (ab/perform-test ab-tracking 0.05)]
        (layout/render "ab-testing-grid.html"
                       {:grid by-group-outcome,
                        :stats (sort (seq stats))
                        :significant (:significant stats)})))))

(defroutes
  ab-testing-routes
  (GET "/ab-testing/" [] (index))
  (DELETE "/ab-testing/" [] (delete-all))
  (GET "/ab-testing/grid/" [] (grid)))
