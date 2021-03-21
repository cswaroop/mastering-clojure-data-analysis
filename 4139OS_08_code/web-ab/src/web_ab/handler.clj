(ns web-ab.handler
  (:require [compojure.core :refer [defroutes]]
            [web-ab.routes.home :refer [home-routes]]
            [web-ab.routes.ab-testing :refer [ab-testing-routes]]
            [web-ab.models.schema :as schema]
            [noir.util.middleware :as middleware]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [web-ab.routes.cljsexample :refer [cljs-routes]]))

(defroutes
  app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info,
     :enabled? true,
     :async? false,
     :max-message-per-msecs nil,
     :fn rotor/append})
  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "web_ab.log", :max-size (* 512 1024), :backlog 10})
  (if (env :selmer-dev) (parser/cache-off!))

  (when-not (schema/initialized?)
    (schema/create-tables))

  (timbre/info "web-ab started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "web-ab is shutting down..."))

(defn template-error-page [handler]
  (if (env :selmer-dev)
    (fn [request]
      (try
        (handler request)
        (catch
          clojure.lang.ExceptionInfo
          ex
          (let [{:keys [type error-template], :as data} (ex-data ex)]
            (if (= :selmer-validation-error type)
              {:status 500, :body (parser/render error-template data)}
              (throw ex))))))
    handler))

(def app
 (middleware/app-handler
   [ab-testing-routes cljs-routes home-routes app-routes]
   :middleware
   [template-error-page]
   :access-rules
   []
   :formats
   [:json-kw :edn]))

