(ns cde.db.core
  "Database connection management and type conversions.

   This namespace establishes the PostgreSQL database connection using Conman
   and defines custom type conversions between Clojure data structures and
   PostgreSQL types.

   Key components:
   - `*db*`: Dynamic var holding the database connection (Mount state)
   - HugSQL query bindings from resources/sql/queries.sql

   Type conversions:
   - PostgreSQL JSON/JSONB <-> Clojure maps
   - PostgreSQL timestamps <-> java.time types
   - PostgreSQL arrays <-> Clojure vectors
   - PostgreSQL citext <-> Clojure strings"
  (:require
   [cheshire.core :refer [generate-string parse-string]]
   [next.jdbc.date-time]
   [next.jdbc.prepare]
   [next.jdbc.result-set]
   [clojure.tools.logging :as log]
   [conman.core :as conman]
   [cde.config :refer [env]]
   [mount.core :refer [defstate]])
  (:import (org.postgresql.util PGobject)))

(defstate ^:dynamic *db*
  "Dynamic var holding the database connection pool.

  Managed by Mount - connects on start, disconnects on stop.
  Connection URL is read from `:database-url` in [[cde.config/env]].

  Used by all db/* namespaces for query execution via Conman."
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn pgobj->clj
  "Converts a PostgreSQL PGobject to a Clojure data structure.
   Handles JSON/JSONB (parsed to maps), citext (to string), and other types."
  [^org.postgresql.util.PGobject pgobj]
  (let [type (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json" (parse-string value true)
      "jsonb" (parse-string value true)
      "citext" (str value)
      value)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

(defn clj->jsonb-pgobj
  "Converts a Clojure map or vector to a PostgreSQL JSONB PGobject.

  Arguments:
  - `value` - Clojure data structure (map or vector)

  Returns: PGobject with type \"jsonb\" containing JSON string."
  [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))
  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))
