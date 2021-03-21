(defproject nullh "0.1.0-snapshot"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.4"]
                 [http.async.client "0.5.2"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.json "0.2.3"]
                 [me.raynes/fs "1.4.5"]
                 [incanter "1.5.4"]
                 [geocoder-clj "0.2.2"]
                 [geo-clj "0.3.5"]
                 [congomongo "0.4.1"]
                 [org.apache.poi/poi-ooxml "3.9"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}})
