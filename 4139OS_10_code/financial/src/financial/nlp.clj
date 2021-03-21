(ns financial.nlp
  (:require [clojure.string :as str]
            [clojure.set :as set])
  (:use [financial types utils]))

(defn tokenize [string]
  (map str/lower-case (re-seq #"[\p{L}\p{M}]+" string)))

(defn tokenize-text [m] (update-in m [:text] tokenize))

(defn token-freqs [m] (update-in m [:text] frequencies))

(defn corpus-freqs [coll]
  (reduce #(merge-with + %1 %2) {} (map :text coll)))

(defn load-stop-words [filename]
  (set (tokenize (slurp filename))))

(defn remove-stop-words [stop-words m]
  (update-in m [:text] #(remove stop-words %)))

(defn keep-white-list [white-list-set m]
  (update-in m [:text] #(filter white-list-set %)))

(defn make-rare-word-list [freqs n]
  (map first (filter #(< (second %) n) freqs)))

(defn process-articles
  ([articles]
   (process-articles articles ["d/english.stop" "d/english.rare"]))
  ([articles stop-files]
   (let [stop-words (reduce set/union #{} (map load-stop-words stop-files))
         process (fn [text] (frequencies (remove stop-words (tokenize text))))]
     (map #(update-in % [:text] process) articles))))

(defn load-articles [white-list-set articles]
  (let [process (fn [text]
                  (frequencies (filter white-list-set (tokenize text))))]
    (map #(update-in % [:text] process) articles)))

(defn tf [term-freq max-freq]
  (+ 0.5 (/ (* 0.5 term-freq) max-freq)))

(defn tf-article [article term]
  (let [freqs (:text article)]
    (tf (freqs term 0) (reduce max 0 (vals freqs)))))

(defn has-term?
  ([term] (fn [a] (has-term? term a)))
  ([term a]
   (not (nil? (get (:text a) term)))))

(defn idf [corpus term]
  (Math/log
    (/ (count corpus)
       (inc (count (filter (has-term? term) corpus))))))

(defn get-vocabulary [corpus]
  (reduce set/union #{} (map #(set (keys (:text %))) corpus)))

(defn get-idf-cache [corpus]
  (reduce #(assoc %1 %2 (idf corpus %2)) {}
          (get-vocabulary corpus)))

(defn tf-idf [idf-value freq max-freq]
  (* (tf freq max-freq) idf-value))

(defn tf-idf-freqs [idf-cache freqs]
  (let [max-freq (reduce max 0 (vals freqs))]
    (into {}
          (map #(vector (first %)
                        (tf-idf
                          (idf-cache (first %))
                          (second %)
                          max-freq))
               freqs))))

(defn tf-idf-over [idf-cache article]
  (update-in article [:text] #(tf-idf-freqs idf-cache %)))

(defn tf-idf-cached [idf-cache corpus]
  (map #(tf-idf-over idf-cache %) corpus))

(defn tf-idf-all [corpus]
  (tf-idf-cached (get-idf-cache corpus) corpus))

(defn load-text-files [token-white-list idf-cache articles]
  (tf-idf-cached idf-cache (load-articles token-white-list articles)))
