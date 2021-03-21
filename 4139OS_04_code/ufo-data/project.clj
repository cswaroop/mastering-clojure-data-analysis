(defproject ufo-data "0.1.0-SNAPSHOT"
  :plugins [[lein-cljsbuild "0.3.2"]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.0"]]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-time "0.5.1"]
                 [incanter "1.5.2"]
                 [cc.mallet/mallet "2.0.7"]
                 [me.raynes/fs "1.4.4"]]
  :cljsbuild
    {:builds [{:source-paths ["src-cljs"],
               :compiler {:pretty-printer true,
                          :output-to "www/js/main.js",
                          :optimizations :whitespace}}]})
