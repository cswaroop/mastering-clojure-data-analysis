(ns social-so.xml
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [social-so.data :as d]
            [social-so.utils :as u])
  (:import [org.jsoup Jsoup]
           [org.jsoup.safety Whitelist]))

(defn read-posts [stream] (:content (xml/parse stream)))

(defn get-user [el]
  (let [{:keys [attrs]} el]
    (or (u/->long (:OwnerUserId attrs))
        (u/to-lower (:OwnerDisplayName attrs)))))

(defn get-post-type [el]
  (let [type-key {1 :question, 2 :answer}]
    (type-key (u/->long (:PostTypeId (:attrs el))))))

(defn is-question [post] (= :question (get-post-type post)))
(defn get-questions [posts] (filter is-question posts))

(defn is-answer [post] (= :answer (get-post-type post)))
(defn get-answers [posts] (filter is-answer posts))

(defn get-score [post] (u/->long (:Score (:attrs post))))
(defn get-id [post] (u/->long (:Id (:attrs post))))
(defn get-accepted-id [post] (u/->long (:AcceptedAnswerId (:attrs post))))
(defn get-body [post] (:Body (:attrs post)))
(defn get-body-text [post]
  (-> post
    get-body
    (Jsoup/clean (Whitelist/none))
    (str/replace "&lt;" "<")
    (str/replace "&gt;" ">")
    (str/replace "&amp;" "&")))

(defn update-user-info [user-id post-el user-info]
  (let [incrs {:question [1 0], :answer [0 1]}
        [q-inc a-inc] (incrs (get-post-type post-el))]
    (cond
      (nil? q-inc) user-info
      (nil? user-info) (d/->UserInfo user-id 1 q-inc a-inc)
      :else
      (assoc user-info
             :post (inc (:post user-info))
             :q (+ (:q user-info) q-inc)
             :a (+ (:a user-info) a-inc)))))

(defn update-user-index [user-index post-el]
  (let [user (get-user post-el)]
    (->> user
      (get user-index)
      (update-user-info user post-el)
      (assoc user-index user))))

(defn load-user-infos [filename]
  (with-open [s (io/input-stream filename)]
    (->> s
      read-posts
      (reduce update-user-index {})
      vals
      (remove nil?)
      doall)))

(defn update-rank [user-property rank-info]
  (let [[rank user-info] rank-info]
    (assoc user-info user-property
           (d/->CountRank (get user-info user-property) rank))))

(defn add-rank-data [user-property users]
  (map #(update-rank user-property %)
       (d/rank-on user-property users)))

(defn add-all-ranks [users]
  (->> users
    (add-rank-data :post)
    (add-rank-data :q)
    (add-rank-data :a)))

(defn load-xml [filename]
  (add-all-ranks (load-user-infos filename)))

