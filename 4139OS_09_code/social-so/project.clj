(defproject social-so "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]
                 [org.clojure/data.json "0.2.4"]
                 [cc.mallet/mallet "2.0.7"]
                 [org.jsoup/jsoup "1.7.3"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}}
  :jvm-opts ["-Xmx2048m"])
