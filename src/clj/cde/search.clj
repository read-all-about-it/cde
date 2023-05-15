(ns cde.search
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn search-titles [query-params]
  (db/search-titles* query-params))