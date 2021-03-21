(defproject web-ab "0.1.0-SNAPSHOT"
  :dependencies [[com.h2database/h2 "1.3.174"]
                 [ring-server "0.3.1"]
                 [domina "1.0.2"]
                 [org.clojure/clojurescript "0.0-2030"]
                 [environ "0.4.0"]
                 [com.taoensso/timbre "2.7.1"]
                 [markdown-clj "0.9.35"]
                 [prismatic/dommy "0.1.2"]
                 [korma "0.3.0-RC6"]
                 [com.taoensso/tower "2.0.0"]
                 [selmer "0.5.3"]
                 [org.clojure/clojure "1.5.1"]
                 [cljs-ajax "0.2.2"]
                 [log4j "1.2.17"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]
                 [compojure "1.1.6"]
                 [lib-noir "0.7.6"]
                 [com.postspectacular/rotor "0.1.0"]
                 [incanter/incanter-core "1.5.4"]]
  :cljsbuild {:builds
              [{:source-paths ["src-cljs"],
                :compiler
                {:pretty-print false,
                 :output-to "resources/public/js/site.js",
                 :optimizations :advanced}}]}
  :ring {:handler web-ab.handler/app,
         :init web-ab.handler/init,
         :destroy web-ab.handler/destroy}
  :profiles {:production {:ring
                          {:open-browser? false,
                           :stacktraces? false,
                           :auto-reload? false}},
             :dev
             {:dependencies [[ring-mock "0.1.5"]
                             [ring/ring-devel "1.2.1"]],
              :env {:selmer-dev true}}}
  :url "http://example.com/FIXME"
  :aot :all
  :plugins [[lein-ring "0.8.7"]
            [lein-environ "0.4.0"]
            [lein-cljsbuild "0.3.3"]]
  :min-lein-version "2.0.0")
