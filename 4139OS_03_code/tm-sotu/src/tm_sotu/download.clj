
(ns tm-sotu.download
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.java.io :as io])
  (:import [java.net URL]
           [java.io File]))

(def index-url "http://www.presidency.ucsb.edu/sou.php")

(defn filter-year-content?
  [tags]
  (filter #(re-matches #"\d{4}" (first (:content %))) tags))

(defn get-index-links
  [index-url]
  (->
    index-url
    enlive/html-resource
    (enlive/select [:.doclist :a])
    filter-year-content?))

(defn get-text-tags
  [page]
  (concat 
    (enlive/select page [:.displaytext])
    (enlive/select page [:p])))

(defn download-get-text
  [tag-map]
  (let [{:keys [tag attrs content]} tag-map]
    (print (str (first content) "\n"))
    (get-text-tags (-> attrs :href URL. enlive/html-resource))))

(defprotocol Textful
  (extract-text [x]
    "This pulls the text from an element. Returns a seq of String."))

(extend-protocol Textful
  java.lang.String
  (extract-text [x] (list x))

  clojure.lang.PersistentStructMap
  (extract-text [x]
      (concat
        (extract-text (:content x))
        (when (contains? #{:span :p} (:tag x))
          ["\n\n"])))

  clojure.lang.LazySeq
  (extract-text [x] (mapcat extract-text x))

  nil
  (extract-text [x] nil))

(defn save-text-seq
  [filename texts]
  (with-open [w (io/writer filename)]
    (doseq [line texts]
      (.write w line))))

(defn unique-filename
  ([basename] (unique-filename basename 0))
  ([basename n]
   (let [filename (str basename \- n ".txt")]
     (if (.exists (File. filename))
       (unique-filename basename (inc n))
       filename))))

(defn process-speech-page
  [outputdir a-tag]
  (->> a-tag
    :attrs
    :href
    URL.
    enlive/html-resource
    get-text-tags
    extract-text
    (save-text-seq (unique-filename (str outputdir \/ (first (:content a-tag)))))))

(defn download-corpus
  ([datadir] (download-corpus datadir index-url))
  ([datadir index-url]
   (doseq [link (get-index-links (URL. index-url))]
     (println (first (:content link)))
     (process-speech-page datadir link))))

