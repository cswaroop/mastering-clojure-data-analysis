(defproject clj-gis "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/fs "1.4.4"]
                 [com.velisco/clj-ftp "0.3.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-time "0.5.1"]
                 [incanter/incanter-charts "1.5.1"]]
  :jvm-opts ["-Xmx4096m"])
