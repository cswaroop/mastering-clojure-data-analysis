
(ns ufo-data.utils)

(def margin {:top 20, :right 20, :bottom 60, :left 40})
(def width (- 960 (:left margin) (:right margin)))
(def height (- 500 (:top margin) (:bottom margin)))

(def format-percent (.format js/d3 ".0%"))

(defn get-bar-scales []
  {:x (.. js/d3 -scale ordinal (rangeRoundBands (array 0 width) 0.1))
   :y (.. js/d3 -scale linear (range (array height 0)))})

(defn get-time-scales []
  {:x (.. js/d3 -time scale (range (array 0 width)))
   :y (.. js/d3 -scale linear (range (array height 0)))})

(defn get-linear-scales []
  {:x (.. js/d3 -scale linear (range (array 0 width)))
   :y (.. js/d3 -scale linear (range (array height 0)))})

(defn axes [x y]
  {:x-axis (.. js/d3 -svg axis
             (scale x)
             (orient "bottom"))
   :y-axis (.. js/d3 -svg axis
             (scale y)
             (orient "left"))})

(defn get-svg []
  (let [{:keys [left right top bottom]} margin]
    (.. js/d3
      (select ".container")
      (append "div")
      (append "svg")
      (attr "width" (+ width left right))
      (attr "height" (+ height top bottom))
      (append "g")
      (attr "transform" (str "translate(" left \, top ")")))))

(defn get-line [x-getter y-getter]
  (.. js/d3 -svg line
    (interpolate "basis")
    (x x-getter)
    (y y-getter)))

(defn caption
  [text width]
  (.. js/d3
    (select ".container div")
    (append "div")
    (style "width" (str width "px"))
    (attr "class" "caption")
    (text text)))

(defn set-caption [caption]
  (.. js/d3
    (select ".caption")
    (text caption)))

(defn setup-x-axis [svg x-axis]
  (.. svg
    (append "g")
    (attr "class" "x axis")
    (attr "transform" (str "translate(0," height \)))
    (call x-axis)))

(defn setup-y-axis
  ([svg y-axis]
   (.. svg
     (attr "class" "y axis")
     (call y-axis)))
  ([svg y-axis caption]
   (.. svg
     (append "g") (attr "class" "y axis") (call y-axis)
     (append "text")
     (attr "transform" "rotate(-90)")
     (attr "y" 6)
     (attr "dy" ".71em")
     (style "text-anchor" "end")
     (text caption))))

(defn set-domains [data x-axis y-axis]
  (let [[x x-getter] x-axis, [y y-getter] y-axis]
    (.domain x (apply array (map x-getter data)))
    (.domain y (array 0 (apply max (map y-getter data))))))

(defn set-extent-domain [data x-axis y-axis]
  (let [[x x-getter] x-axis, [y y-getter] y-axis]
    (.domain x (.extent js/d3 data x-getter))
    (.domain y (array 0 (.max js/d3 data y-getter)))))

