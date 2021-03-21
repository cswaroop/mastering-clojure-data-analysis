(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [clojure.data.xml :as xml]
    [social-so]
    [social-so.data :as d]
    [social-so.nlp :as n]
    [social-so.output :as out]
    [social-so.post :as p]
    [social-so.utils :as u]
    [social-so.xml :as x]))

(def system
  "A Var containing an object representing the application under
  development."
  {:filename "data/stackoverflow.com-Posts"
   :sample-file "post-sample-100000.json"
   :post-infos nil})

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root
    #'system
    (fn [s]
      (println "Reading" (:sample-file s))
      (assoc s :post-infos (p/load-post-infos (:sample-file s))))))

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

