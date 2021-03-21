
(ns sentiment.x-valid)

(defn partition-spread [k coll]
  (let [get-mod (fn [i x]
                  [(mod i k) x])
        map-second #(map second (second %))]
    (->> coll
      (map-indexed get-mod)
      (group-by first)
      (map map-second))))

(defn step-folds-seq [folds steps]
  (lazy-seq
    (when-let [[s & ss] (seq steps)]
      (let [[prefix [validation & suffix]] (split-at s folds)
            training (flatten (concat prefix suffix))
            current [validation training]]
        (cons current (step-folds-seq folds ss))))))

(defn step-folds [folds]
  (step-folds-seq folds (range (count folds))))

(defn k-fold
  ([train error combine data] (k-fold train error combine 10 data))
  ([train error combine k input-data]
   (->> input-data
     shuffle
     (partition-spread k)
     step-folds
     (map (fn [[v t]] [v (train t)]))
     (map (fn [[v t]] (let [err (error t v)]
                        (println :error err)
                        err)))
     (reduce combine (combine)))))

(defn accum-error [error-counts pair]
  (let [get-key {["+" "+"] :true-pos
                 ["-" "-"] :true-neg
                 ["+" "-"] :false-neg
                 ["-" "+"] :false-pos}
        k (get-key pair)]
    (assoc error-counts k (inc (error-counts k)))))

(defn summarize-error [error-counts]
  (let [{:keys [true-pos false-pos true-neg false-neg]} error-counts]
    {:precision (float (/ true-pos (+ true-pos false-pos))),
     :recall (float (/ true-pos (+ true-pos false-neg)))}))

(defn compute-error [expecteds actuals]
  (let [start {:true-neg 0, :false-neg 0, :true-pos 0, :false-pos 0}]
    (summarize-error
      (reduce accum-error start
              (map vector expecteds actuals)))))

(defn mean-error [coll]
  (let [start {:precision 0, :recall 0}
        accum (fn [a b]
                {:precision (+ (:precision a) (:precision b))
                 :recall (+ (:recall a) (:recall b))})
        summarize (fn [n a]
                    {:precision (/ (:precision a) n)
                     :recall (/ (:recall a) n)})]
    (summarize (count coll) (reduce accum start coll))))

(defn f-score [error]
  (let [{:keys [precision recall]} error]
    (* 2 (/ (* precision recall) (+ precision recall)))))

