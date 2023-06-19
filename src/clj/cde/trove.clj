(ns cde.trove
  "Code for interfacing with the Trove API"
  (:require
   [cde.config :refer [env]]))


;; Code for interfacing with the Trove API

(defn- trove-api-url [endpoint]
  ;; TODO - use a proper Clojure library for building URLs
  ;; TODO - test this
  ;; TODO - spec this with {:pre [] :post []}
  ;; TODO - decide how to access private trove api key
  (str "https://api.trove.nla.gov.au/v2" endpoint
      "?key=" (first (get-in env [:config :trove-api-keys]))
       "&encoding=json"))

(defn- trove-newspaper-url
  "Return the trove API URL for a newspaper with the given id."
  [id]
  (trove-api-url (str "/newspaper/title/" id)))


(defn- trove-article-url
  "Return the trove API URL for an article with the given id."
  [id]
  (trove-api-url (str "/newspaper/" id)))