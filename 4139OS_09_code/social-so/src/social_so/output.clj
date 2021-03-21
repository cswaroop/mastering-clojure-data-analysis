(ns social-so.output)

(defn print-top-rank [col-name key-fn n users]
  (let [fmt "%4s   %6s   %14s\n"
        sort-fn #(:rank (key-fn %))]
    (printf fmt "Rank" "User" col-name)
    (printf fmt "----" "------" "--------------")
    (doseq [user (take n (sort-by sort-fn users))]
      (let [freq (key-fn user)]
        (printf fmt (:rank freq) (:user user) (:count freq))))))

(defn print-rank-table [users]
  (let [fmt "%6s   %9s   %14s   %12s\n"]
    (printf fmt "Users" "All Posts" "Question Posts" "Answer Posts")
    (printf fmt "------" "---------" "--------------" "------------")
    (doseq [{:keys [user post q a]} users]
      (printf fmt user (:rank post) (:rank q) (:rank a)))))

