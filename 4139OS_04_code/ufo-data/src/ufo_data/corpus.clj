(ns ufo-data.corpus
  (:require
    [ufo-data.text :as t]))

(defn divide-corpus [coll token]
  (group-by #(contains? % token) (map t/get-bag coll)))
