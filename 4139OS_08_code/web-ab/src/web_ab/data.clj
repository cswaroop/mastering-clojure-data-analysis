(ns web-ab.data
  (:import [java.util Date])
  (:use korma.core)
  (:require [web-ab.models.db :as db]))

(def ^:dynamic *p* 0.2)
(def day (* 1000 60 60 24))
(def min-5 (* 1000 60 5))

(defn time-near
  ([] (time-near (Date.) day))
  ([start] (time-near start day))
  ([start time-range]
   (let [d (.clone start)
         offset (- (int (rand time-range)) (/ time-range 2))]
     (.setTime d (+ offset (.getTime start)))
     d)))

(defn rand-time [] (time-near))

(defn rand-time-p
  ([] (rand-time-p *p*))
  ([p]
   (when (<= (rand) p)
     (rand-time))))

(defn rand-time-offset-p
  ([start] (rand-time-offset-p start min-5 *p*))
  ([start time-range p]
   (when (<= (rand) p)
     (doto (.clone start)
       (.setTime (+ (int (rand time-range)) (.getTime start)))))))

(defn insert-ab-tracking [test-group start-at succeed]
  (insert db/abtracking (values [{:testgroup (db/group-code test-group)
                                  :startat start-at
                                  :succeed succeed}])))

(defn rand-group [] (rand-nth (keys db/group-code)))

(defn insert-rand-tracking
  ([] (insert-rand-tracking (constantly *p*)))
  ([p]
   (let [group (rand-group)
         start (rand-time)]
     (insert-ab-tracking
       group start (rand-time-offset-p start min-5 (p group))))))

(defn insert-rand-until [pred p-vals]
  (delete db/abtracking)
  (loop [n 0]
    (when (zero? (mod n 1000))
      (println :insert-rand-until n))
    (when-not (pred)
      (insert-rand-tracking p-vals)
      (recur (inc n)))))

(comment

(require '[web-ab.routes.ab-testing :as r :reload-all true])
(require '[web-ab.data :as d :reload-all true])
(import [java.util Date])

(d/rand-time-offset-p (Date.) d/min-5 ({:control 0.2, :test 0.24} :control))

(d/insert-rand-until r/is-over? {:control 0.2, :test 0.24})

  )

