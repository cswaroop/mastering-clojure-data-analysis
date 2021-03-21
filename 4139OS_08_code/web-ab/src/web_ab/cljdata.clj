(ns web-ab.cljdata)


(defn get-target-sample
  "This returns the target number of subjects needed for each variable in the
  test.

  * rate is the base rate for the control.
  * min-effect is the absolute minimum effect that we want to test for.
  "
  [rate min-effect]
  (let [v (* rate (- 1.0 rate))]
    (* 16.0 (/ v (* min-effect min-effect)))))


