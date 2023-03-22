(ns cde.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [cde.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[cde started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[cde has shut down successfully]=-"))
   :middleware wrap-dev})
