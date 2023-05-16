(ns cde.newspaper
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn create-newspaper! [params]
  (db/create-newspaper!* params))