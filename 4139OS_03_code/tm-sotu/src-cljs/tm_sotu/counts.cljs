
(ns tm-sotu.counts
  (:require [tm-sotu.utils :as utils]
            [clojure.browser.dom :as dom]))

(defn get-count [d] (.-count d))
(defn get-year [d] (.-year d))
(defn get-instance [d] (.-instance d))

(defn parse-datum [d]
  (aset d "year" (utils/parse-instance (.-instance d)))
  (aset d "count" (long (.-count d)))
  d)

(defn ^:export word-counts []
  (let [{:keys [x y]} (utils/get-scales)
        {:keys [x-axis y-axis]} (utils/axes x y)
        color (.hsl js/d3 205 0.71 0.41)
        line (utils/get-line (comp x get-year) (comp y get-count))
        svg (utils/get-svg)]
    (utils/caption "Word Counts of SOTU Addresses" 275)
    (.csv js/d3
          "word-counts.csv"
          (fn [error data]
            (let [data (into-array (map parse-datum data))]
              (utils/set-domains data [x get-year] [y get-count])
              (utils/setup-x-axis svg x-axis)
              (utils/setup-y-axis svg y-axis "Word Counts")
              (.. svg
                (append "path")
                (datum data)
                (attr "class" "line")
                (attr "d" line)))))))

