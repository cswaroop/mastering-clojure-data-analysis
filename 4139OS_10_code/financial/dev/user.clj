(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [clj-time.core :as time]
    [clj-time.coerce :as time-coerce]
    [clj-time.format :as time-format]
    [enclog.nnets :as nnets]
    [enclog.training :as training]
    [me.raynes.fs :as fs]
    [financial]
    [financial.types :as t]
    [financial.nlp :as nlp]
    [financial.nn :as nn]
    [financial.oanc :as oanc]
    [financial.csv-data :as csvd]
    [financial.utils :as u]))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  ;; TODO
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root
    #'system (fn [s]
               (let [_ (println :slate)
                     slate (doall
                             (map oanc/load-article
                                  (oanc/find-slate-files
                                    (io/file "d/OANC-GrAF"))))
                     _ (println :freqs)
                     freqs (nlp/tf-idf-all (nlp/process-articles slate))
                     _ (println :stocks)
                     stocks (csvd/read-stock-prices "d/d-1996-2001.csv")
                     _ (println :stock-index)
                     stock-index (nn/index-by :date stocks)
                     _ (println :vocab)
                     vocab (doall (nlp/get-vocabulary freqs))
                     _ (println :training)
                     training (doall (nn/make-training-set stock-index vocab freqs))]
                 (println :done)
                 {:slate slate,
                  :freqs freqs,
                  :stock stocks,
                  :stock-index stock-index,
                  :vocab vocab,
                  :training training}))))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  ;; TODO
  )

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))
