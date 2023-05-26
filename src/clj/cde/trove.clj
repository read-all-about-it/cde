(ns cde.trove
  (:require
   [cde.config :refer [env]]))


;; Code for interfacing with the Trove API

(defn- trove-api-url [endpoint]
  ;; TODO - use a proper Clojure library for building URLs
  ;; TODO - test this
  ;; TODO - spec this with {:pre [] :post []}
  ;; TODO - decide how to access private trove api key
  (str "https://api.trove.nla.gov.au/v2/" endpoint
    ;;   "?key=" (get-in env [:config :trove-api-key])
       "&encoding=json"))