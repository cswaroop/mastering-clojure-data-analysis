(defproject financial "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-time "0.6.0"]
                 [me.raynes/fs "1.4.4"]
                 [org.encog/encog-core "3.1.0"]
                 [enclog "0.6.3"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}})
