(defproject sentiment "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "0.3.2"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.json "0.2.3"]
                 [org.apache.opennlp/opennlp-tools "1.5.3"]
                 [nz.ac.waikato.cms.weka/weka-dev "3.7.7"]]
  :repositories [["dev.davidsoergel.com releases"
                  {:url "http://dev.davidsoergel.com/artifactory/repo"
                   :snapshots false}]
                 ["dev.davidsoergel.com snapshots"
                  {:url "http://dev.davidsoergel.com/artifactory/repo"
                   :releases false}]]
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:pretty-printer true
                                   :output-to "www/js/main.js"
                                   :optimizations :whitespace}}]}
  :jvm-opts ["-Xmx4096m"])
