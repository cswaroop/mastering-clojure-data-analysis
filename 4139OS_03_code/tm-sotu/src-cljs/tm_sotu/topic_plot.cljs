
(ns tm-sotu.topic-plot
  (:require [tm-sotu.utils :as utils]
            [clojure.browser.dom :as dom]
            [clojure.string :as str]))

(defn parse-datum [d]
  (aset d "instance" (utils/parse-instance (.-instance d)))
  (aset d "weighting" (js/Number (.-distribution d)))
  d)

(defn get-topic [d] (.-topic d))
(defn get-year [d] (.-year d))
(defn get-weighting [d] (.-weighting d))
(defn get-instance [d] (.-instance d))
(defn get-key [d] (.-key d))
(defn get-name [d] (.-name d))
(defn get-values [d] (.-values d))

(defn make-key-box []
  (.. js/d3
    (select ".container div")
    (append "div")
    (attr "id" "keybox")
    (style "border" "1px solid black")
    (style "float" "right")
    (style "margin-top" "150px")
    (style "margin-right" "50px")
    (style "width" "150px")
    (style "height" "300px")))

(defn parse-word-weight [d]
  (aset d "weight" (long (.-weight d)))
  d)

(deftype YearWeighting [year weighting])

(deftype Topic [topic values])

(defn make-topic [data t]
  (Topic. t
          (into-array
            (map #(YearWeighting. (.-instance %)
                                  (.-weighting %))
                 (.filter data #(= (.-topic %) t))))))

(deftype TopicText [name value])

(defn make-text [d]
  (TopicText. (.-topic d) (last (.-values d))))

(defn table-row [d]
  (str "<tr><th>" (.-word d) "</th><td>" (.-weight d) "</td></tr>"))

(defn get-topic-selection []
  (let [select (dom/get-element :legend-select)]
    (.-value (aget (.-options select) (.-selectedIndex select)))))

(defn update-word-list [tree topic-n]
  (let [d (first (filter #(= (.-key %) topic-n) tree))
        buffer (concat
                 ["<table id='word-list' style='margin-left: 10px' padding='2'>"]
                 (map table-row (.-values d))
                 ["</table>"])]
    (.html (js/$ "#legend") (str/join buffer))
    (.attr (js/$ ".line") "opacity" "0.05")
    (.attr (js/$ (str "#line" (.-key d))) "opacity" "1.0")))

(defn instance->str [inst]
  (let [[base suffix] (.split (+ "" inst) ".")]
    (str base \- (if (nil? suffix) "0" "1"))))

(defn update-instance-table [data topic-n]
  (.. js/d3 (select "#instance-table tbody") remove)
  (.. js/d3 (select "#instance-table")
    (append "tbody")
    (selectAll "tr")
    (data (.-values (aget data topic-n)))
    (enter)
    (append "tr")
    (html (fn [d]
            (str "<th style='text-align: left;'>"
                 "<a href='data/" (instance->str (.-instance d)) ".txt'>"
                 (.-instance d) "</a></th><td>" (.-weighting d)
                 "</td>")))))

(defn make-tree [data]
  (.. js/d3 nest
    (key get-topic)
    (sortValues (fn [a b]
                  (cond (< (.-weight b) (.-weight a)) -1 
                        (> (.-weight b) (.-weight a)) 1
                        :else 0)))
    (entries data)))

(defn make-instance-table []
  (.. js/d3 (select ".container")
    (append "div")
    (attr "width" (+ utils/width (:right utils/margin) (:left utils/margin)))
    (append "table")
    (attr "id" "instance-table"))
  (let [thead (.. js/d3 (select "#instance-table")
                (append "thead")
                (append "tr"))]
    (.. thead (append "th") (text "Year"))
    (.. thead (append "th") (text "Weighting")))
  (.. js/d3 (select "#instance-table") (append "tbody")))

(defn make-key-box-html [key-box tree weightings]
  (.. key-box
    (append "select")
    (attr "id" "legend-select")
    (style "width" "125px")
    (style "margin-top" "3px")
    (style "margin-left" "13px")
    (on "change" (fn []
                   (let [t (get-topic-selection)]
                     (update-word-list tree t)
                     (update-instance-table weightings t)))))
  (make-instance-table))

(defn populate-select [tree]
  (.. js/d3 (select "#legend-select")
    (selectAll "option") (data tree)
    (enter) (append "option")
    (attr "value" get-key)
    (text #(str "Topic " (get-key %)))))

(defn add-key-div [key-box]
  (.. key-box
    (append "div")
    (attr "id" "legend")))

(defn load-key-box [weightings data]
  (let [data (into-array (map parse-word-weight data))
        key-box (.select js/d3 "#keybox")
        tree (make-tree data)]
    (make-key-box-html key-box tree weightings)
    (populate-select tree)
    (add-key-div key-box)))

(defn make-topic-svg [svg topics]
  (.. svg
    (selectAll ".topic")
    (data topics)
    (enter)
    (append "g")
    (attr "class" "topic")))

(defn add-topic-lines [line color topic-svg]
  (.. topic-svg
    (append "path")
    (attr "class" "line")
    (attr "id" #(str "line" (.-topic %)))
    (attr "d" #(line (.-values %)))
    (style "stroke" #(color (.-topic %)))))

(defn add-topic-labels [topic-svg x y]
  (.. topic-svg
    (append "text")
    (datum make-text)
    (attr "transform" #(str "translate(" (x (.-year (.-value %)))
                            \, (y (.-weighting (.-value %))) \)))
    (attr "x" 3)
    (attr "dy" ".35em")
    (text get-name)))

(defn load-topic-weights [svg line color x x-axis y y-axis data]
  (let [data (into-array (map parse-datum data))]
    (.domain color (into-array (set (map get-topic data))))
    (let [topics (into-array
                   (map #(make-topic data %) (.domain color)))
          wghts (map get-weighting data)
          weightings (.. js/d3 nest
                       (key get-topic)
                       (sortValues (fn [a b]
                                     (let [a (.-instance a)
                                           b (.-instance b)]
                                       (cond (< a b) -1
                                             (> a b) 1
                                             :else 0))))
                       (entries data))]
      (.domain x (.extent js/d3 data get-instance))
      (.domain y (array (apply min wghts) (apply max wghts)))
      (utils/setup-x-axis svg x-axis)
      (utils/setup-y-axis svg y-axis "Weightings")
      (let [topic-svg (make-topic-svg svg topics)]
        (add-topic-lines line color topic-svg)
        (add-topic-labels topic-svg x y)
        (utils/caption
          (str "Topic Weightings over Time (topic count = "
               (count topics) \))
          650)
        (.csv js/d3 "topic-words.csv"
              (partial load-key-box weightings))))))

(defn ^:export plot-topics []
  (let [{:keys [x y]} (utils/get-scales)
        {:keys [x-axis y-axis]} (utils/axes x y)
        color (.. js/d3 -scale category20)
        line (utils/get-line #(x (get-year %))
                             #(y (get-weighting %)))
        svg (utils/get-svg)]
    (make-key-box)
    (.csv js/d3 "topic-dists.csv"
          (partial load-topic-weights
                   svg line color x x-axis y y-axis))))

