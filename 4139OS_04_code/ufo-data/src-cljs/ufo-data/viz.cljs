
(ns ufo-data.viz
  (:require [ufo-data.utils :as u]
            [clojure.browser.dom :as dom]))

(def term-slice-size 75)

(defn get-shape [d] (.-shape d))
(defn get-count [d] (.-count d))

(defn ^:export term-freqs []
  (let [{:keys [x y]} (u/get-bar-scales)
        {:keys [x-axis y-axis]} (u/axes x y)
        svg (u/get-svg)]
    (u/caption "Frequencies of Shapes" 300)
    (.json
      js/d3
      "term-freqs.json"
      (fn [err json-data]
        (u/set-domains json-data [x get-shape] [y get-count])
        (u/setup-x-axis svg x-axis)
        (u/setup-y-axis svg y-axis "")
        (.. svg
          (selectAll ".bar") (data json-data)
          (enter)
          (append "rect")
          (attr "id" #(str "id" (get-shape %)))
          (attr "class" "bar")
          (attr "x" (comp x get-shape))
          (attr "y" (comp y get-count))
          (attr "width" (.rangeBand x))
          (attr "height" #(- u/height (y (get-count %)))))))))

(defn ^:export descr-freqs []
  (let [{:keys [x y]} (u/get-bar-scales)
        {:keys [x-axis y-axis]} (u/axes x y)
        svg (u/get-svg)
        get-descr #(.-descr %)]
    (u/caption "Frequencies of Description Words" 450)
    (.json
      js/d3
      "descr-freqs.json"
      (fn [err data]
        (let [data (.slice data 0 term-slice-size)]
          (u/set-domains data [x get-descr] [y get-count])
          (u/setup-x-axis svg x-axis)
          (u/setup-y-axis svg y-axis "")
          (.. svg
            (selectAll ".bar") (data data)
            (enter)
            (append "rect")
            (attr "id" #(str "id" (get-descr %)))
            (attr "class" "bar")
            (attr "x" (comp x get-descr))
            (attr "y" (comp y get-count))
            (attr "width" (.rangeBand x))
            (attr "height" #(- u/height (y (get-count %))))))))))

(defrecord Point [year freq])
(defrecord TermData [term values])
(defrecord TermDatum [term value])

(defn get-year [p] (.-year p))
(defn get-freq [p] (.-freq p))

(defn year->Point
  ([term] (partial year->Point term))
  ([term year-data]
   (let [[year data] year-data
         v (if-let [v (aget data term)]
             v
             0)]
     (Point. (js/Date. (str year)) v))))

(defn get-term-data [data term]
  (TermData. term (apply array (map (year->Point term) data))))

(defn index-term [index term-data]
  (.set index (.-term term-data) term-data)
  index)

(defn pivot-year-freqs [data term-set]
  (reduce index-term (.map js/d3) (.. data
                                    -terms
                                    (filter #(contains? term-set %))
                                    (map (partial get-term-data (.-freqs data))))))

(defn text-transform [x y term-datum]
  (str "translate(" (x (.. term-datum -value -x))
       \, (y (/ (+ (.. term-datum -value -y0) (.. term-datum -value -y)) 2.0))
       \)))

(defn update-value-with
  ([f] (partial update-value-with f))
  ([f m]
   (aset m "value" (f (.-value m)))
   m))

(defn set-pair [index pair]
  (.set index (.-key pair) (.-value pair))
  index)

(defn sum-freqs [freqs]
  (reduce + (map #(.-freq %) freqs)))

(defn get-term-totals [by-term]
  (->> by-term
    .entries
    (map (update-value-with #(sum-freqs (.-values %))))
    (reduce set-pair (.map js/d3))))

(defn get-year-array [data]
  (.. data
    -freqs
    (map #(js/Date. (str (first %))))))

(defn set-year-freq-domains [x y data years]
  (.domain x (.extent js/d3 years))
  (.domain y (array 0 (apply max (->> data
                                   .-freqs
                                   (map second)
                                   (mapcat #(.values js/d3 %)))))))

(defn create-bar [sel width x y]
  (.. sel
    (append "rect")
    (attr "class" "bar")
    (attr "x" (comp x get-year))
    (attr "width" width)
    (attr "y" (comp y get-freq))
    (attr "height" #(- u/height (y (get-freq %))))))

(defn update-bar [sel y]
  (.. sel
    (attr "y" (comp y get-freq))
    (attr "height" #(- u/height (y (get-freq %))))))

(defn create-year-freq-bars [svg x y years by-term term]
  (let [bar-width (/ u/width (count years))
        bars (.. svg
               (selectAll ".bar")
               (data (.-values (.get by-term term))))]
    (update-bar bars y)
    (create-bar (.enter bars) bar-width x y)))

(defn get-term-selection []
  (let [select (dom/get-element :term-select)]
    (.-value (aget (.-options select) (.-selectedIndex select)))))

(defn populate-select [by-term terms]
  (let [terms (.. (get-term-totals by-term)
                (entries)
                (sort (fn [a b]
                        (let [av (.-value a), bv (.-value b)]
                          (cond (< bv av) -1
                                (> bv av) 1
                                :else 0)))))]
    (.. js/d3 (select "#term-select")
      (selectAll "option")
      (data terms)
      (enter)
      (append "option")
      (attr "value" #(.-key %))
      (text #(str (.-key %) " (" (.-value %) \))))))

(defn set-caption [term]
  (.. js/d3
    (select ".caption")
    (text (str "Frequency of Term over Time (" term \)))))

(defn add-select [svg x y years by-term terms]
  (.. js/d3 (select ".container")
    (append "div")
    (attr "class" "terms")
    (append "div")
    (style "font-size" "larger")
    (style "font-weight" "bold")
    (text "Terms"))
  (.. js/d3 (select ".terms")
    (append "div")
    (append "select")
    (attr "id" "term-select")
    (style "width" "125px")
    (on "change"
        (fn []
          (let [term (get-term-selection)]
            (set-caption term)
            (create-year-freq-bars
              svg x y years by-term term)))))
  (populate-select by-term terms))

(defn ^:export year-freqs []
  (let [{:keys [x y]} (u/get-time-scales)
        {:keys [x-axis y-axis]} (u/axes x y)
        color "steelblue"
        svg (u/get-svg)]
    (u/caption "Frequency of Term over Time (light)" 500)
    (.json
      js/d3
      "year-freqs.json"
      (fn [err data]
        (let [terms (.-terms data)
              term-set (set terms)
              years (get-year-array data)
              by-term (pivot-year-freqs data term-set)]
          (set-year-freq-domains x y data years)
          (u/setup-x-axis svg x-axis)
          (u/setup-y-axis svg y-axis "Counts")
          (create-year-freq-bars svg x y years by-term "light")
          (add-select svg x y years by-term terms))))))

(defn create-year-hist-bars [svg x y data]
  (let [width (- (/ u/width (count data)) 3)]
    (.. svg
      (selectAll ".bar")
      (data data)
      (enter)
      (append "rect")
      (attr "class" "bar")
      (attr "x" #(x (.-year %)))
      (attr "data-year" #(.-year %))
      (attr "width" width)
      (attr "y" #(y (.-count %)))
      (attr "data-count" #(.-count %))
      (attr "height" #(- u/height (y (.-count %)))))))

(defn ^:export year-hist []
  (let [{:keys [x y]} (u/get-linear-scales)
        {:keys [x-axis y-axis]} (u/axes x y)
        color "steelblue"
        svg (u/get-svg)]
    (u/caption "Frequency of Sightings by Year" 400)
    (.json
      js/d3
      "year-hist.json"
      (fn [err data]
        (let [data (.filter data #(< 1900 (.-year %)))]
          (.sort data (fn [a b]
                        (let [a-year (.-year a) b-year (.-year b)]
                          (cond (< a-year b-year) -1
                                (> a-year b-year) 1
                                :else 0))))
          (.tickFormat x-axis (.format js/d3 "d"))
          (u/set-extent-domain data [x #(.-year %)] [y #(.-count %)])
          (u/setup-x-axis svg x-axis)
          (u/setup-y-axis svg y-axis "Frequency")
          (create-year-hist-bars svg x y data))))))

(defn create-month-hist-bars [svg x y data]
  (let [width (- (/ u/width (count data)) 3)]
    (.. svg
      (selectAll ".bar")
      (data data)
      (enter)
      (append "rect")
      (attr "class" "bar")
      (attr "x" #(x (.-month %)))
      (attr "data-month" #(.-month %))
      (attr "width" width)
      (attr "y" #(y (.-count %)))
      (attr "data-count" #(.-count %))
      (attr "height" #(- u/height (y (.-count %)))))))

(defn ^:export month-hist []
  (let [{:keys [x y]} (u/get-linear-scales)
        {:keys [x-axis y-axis]} (u/axes x y)
        color "steelblue"
        svg (u/get-svg)]
    (u/caption "Frequency of Sightings by Month" 400)
    (.json
      js/d3
      "month-hist.json"
      (fn [err data]
        (.sort data (fn [a b]
                      (let [a-month (.-month a) b-month (.-month b)]
                        (cond (< a-month b-month) -1
                              (> a-month b-month) 1
                              :else 0))))
        (.tickFormat x-axis (.format js/d3 "d"))
        (.domain x (array 1 13))
        (.domain y (array 0 (.max js/d3 data #(.-count %))))
        (u/setup-x-axis svg x-axis)
        (u/setup-y-axis svg y-axis "Frequency")
        (create-month-hist-bars svg x y data)))))

