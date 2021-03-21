
(ns tm-sotu.utils)

(def margin {:top 20, :right 80, :bottom 30, :left 50})
(def width (- 960 (:left margin) (:right margin)))
(def height (- 500 (:top margin) (:bottom margin)))

(defn parse-instance
  "This parses an instance ID into a (fractional) year."
  [instance]
  (+ (long (.substring instance 0 4))
     (if (zero? (long (.substring instance 5)))
       0.0
       0.5)))

(defn get-scales []
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
  (let [{:keys [left right top bottom]} margin
        svg (.. js/d3
              (select ".container")
              (append "div")
              (append "svg")
              (attr "width" (+ width left right))
              (attr "height" (+ height top bottom))
              (append "g")
              (attr "transform" (str "translate(" left \, top ")")))]
    (.. js/d3
      (select ".container")
      (append "div")
      (style "width" (str (+ width left right) "px")))
    svg))

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

(defn setup-x-axis [svg x-axis]
  (.. svg
    (append "g")
    (attr "class" "x axis")
    (attr "transform" (str "translate(0," height \)))
    (call x-axis)))

(defn setup-y-axis [svg y-axis caption]
  (.. svg
    (append "g") (attr "class" "y axis") (call y-axis)
    (append "text")
    (attr "transform" "rotate(-90)")
    (attr "y" 6)
    (attr "dy" ".71em")
    (style "text-anchor" "end")
    (text caption)))

(defn set-domains [data x-axis y-axis]
  (let [[x x-getter] x-axis, [y y-getter] y-axis]
    (.domain x (.extent js/d3 data x-getter))
    (.domain y (.extent js/d3 data y-getter))))

