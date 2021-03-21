(ns nullh.utils
  (:require [me.raynes.fs :as fs]))

(defn ls [dirname]
  "List the directory and return the results joined with the
  dirname."
  (map #(str dirname \/ %) (fs/list-dir dirname)))

(defn pad
  ([n x] (pad n x \space))
  ([n x with]
   (let [x-str (str x)
         len (count x-str)]
     (if (< len n)
       (apply str (conj (vec (repeat (- n len) with)) x-str))
       x-str))))

(defn pad-vec
  ([n v] (pad-vec n v nil))
  ([n v with] (vec (take n (concat v (repeat with))))))

(defn watch-err [f x]
  (try
    (f x)
    (catch Exception ex
      (println "ERROR ON" x)
      (throw ex))))

(defn any? [f coll]
  (reduce #(and %1 (f %2)) false coll))

;;; From http://stackoverflow.com/a/4831170
(defn find-item [x xs]
  (keep-indexed #(when (= %2 x) %1) xs))

