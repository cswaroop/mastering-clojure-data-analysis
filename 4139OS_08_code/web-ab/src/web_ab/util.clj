(ns web-ab.util
  (:require [noir.io :as io]
            [markdown.core :as md]))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))

(defn tee [& msg]
  (fn [x]
    (apply print msg)
    (println \space x)
    x))

(defn on-second
  ([f] (partial on-second f))
  ([f pair]
   (let [[x y] pair]
     [x (f y)])))
