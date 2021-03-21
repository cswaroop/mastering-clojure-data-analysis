(ns web-ab.ab-testing
  (:require [noir.cookies :as c]
            [taoensso.timbre :as timbre]
            [web-ab.models.db :as db]
            [incanter.stats :as s]
            [clojure.set :as set]
            [web-ab.util :as util])
  (:import [java.lang Math]))

(defn get-abcode-cookie []
  (Long/parseLong (c/get :abcode "-1")))

(defn get-previous-copy [ab-code test-cases]
  (-> ab-code
    db/get-abtracking
    :testgroup
    db/code->group
    test-cases))

(defn new-test [test-cases]
  (let [[group text] (rand-nth (seq test-cases))
        ab-tracking (db/get-abtracking (db/create-abtracking group))]
    (timbre/info :ab-tracking ab-tracking)
    (c/put! :abcode (:id ab-tracking))
    text))

(defn start-test [counter default test-cases]
  (let [c (Long/parseLong (c/get counter "0"))
        ab-code (get-abcode-cookie)]
    (c/put! counter (inc c))
    (cond
      (and (>= ab-code 0) (> c 0))
      (get-previous-copy ab-code test-cases)

      (and (< ab-code 0) (> c 0)) default

      :else (new-test test-cases))))

(defn mark-succeed []
  (let [ab-code (get-abcode-cookie)]
    (when (> ab-code -1)
      (db/mark-succeed ab-code))))

(defn pct [x y] (* 100.0 (/ x y)))

(defn succeeded? [ab] (not (nil? (:succeed ab))))
(defn ->keyword [b] (keyword (str b)))
(defn group-by-group [coll] (group-by :group-name coll))

(defn group-by-succeed [coll]
  (group-by #(->keyword (succeeded? %)) coll))

(defn get-results-grid [ab-tracking]
  (->> ab-tracking
    group-by-group
    (map (util/on-second group-by-succeed))
    (into {})))

(defn assoc-totals [m]
  (->> m
    vals
    (map count)
    (reduce + 0)
    (assoc m :total)))

(defn assoc-grid-totals [grid-map]
  (->> grid-map
    seq
    (map (util/on-second assoc-totals))
    (into {})))

(defn get-count [m group result] (count (result (group m))))

(defn to-marker [v] (if (nil? v) 0.0 1.0))
(defn to-flags [group] (map to-marker (map :succeed group)))

(defn get-ab-tracking []
  (map #(assoc % :group-name (db/code->group (:testgroup %)))
       (db/get-all-abtracking)))

(defn binomial-mean [coll] (reduce + 0.0 coll))

(defn binomial-variance [coll]
  (let [n (count coll),
        p (/ (count (remove zero? coll)) n)]
    (* n p (- 1.0 p))))

(defn binomial-se [coll-t coll-c]
  (Math/sqrt (+ (/ (binomial-variance coll-t) (count coll-t))
                (/ (binomial-variance coll-c) (count coll-c)))))

(defn binomial-t-test [coll-t coll-c]
  (/ (- (binomial-mean coll-t) (binomial-mean coll-c))
     (binomial-se coll-t coll-c)))

(defn degrees-of-freedom [coll-t coll-c]
  (let [var-t (binomial-variance coll-t), n-t (count coll-t),
        var-c (binomial-variance coll-c), n-c (count coll-c)]
    (/ (Math/pow (+ (/ var-t n-t) (/ var-c n-c)) 2)
       (+ (/ (* var-t var-t) (* n-t n-t (dec n-t)))
          (/ (* var-c var-c) (* n-c n-c (dec n-c)))))))

(defn perform-test
  ([ab-testing] (perform-test ab-testing 0.05))
  ([ab-testing p]
   (let [groups (group-by-group ab-testing),
         group-t (to-flags (:test groups))
         group-c (to-flags (:control groups))
         t (binomial-t-test group-t group-c)
         df (degrees-of-freedom group-t group-c)
         p-value (- 1.0 (s/cdf-t t :df df))]
     {:control-n (count group-c)
      :control-p (double (/ (count (remove zero? group-c))
                            (count group-c)))
      :test-n (count group-t)
      :test-p (double (/ (count (remove zero? group-t))
                         (count group-t)))
      :control-mean (binomial-mean group-c)
      :test-mean (binomial-mean group-t)
      :control-variance (binomial-variance group-c)
      :test-variance (binomial-variance group-t)
      :se (binomial-se group-t group-c)
      :t-value t, :df df, :p-value p-value, :p-target p,
      :significant (<= p-value p)})))

