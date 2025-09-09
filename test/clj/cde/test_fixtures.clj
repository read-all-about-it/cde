(ns cde.test-fixtures
  "Common test fixtures for database and mount setup"
  (:require
   [mount.core :as mount]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [next.jdbc :as jdbc]
   [cde.config :refer [env]]
   [cde.db.core :as db]
   [cde.handler :as handler]))

(defn load-test-config []
  "Load test configuration from test-config.edn"
  (let [config-file (io/resource "test-config.edn")]
    (if config-file
      (edn/read-string (slurp config-file))
      ;; Fallback to minimal test config if file doesn't exist
      {:database-url "postgresql://localhost/cde_test?user=g"
       :auth0 {:domain "test.auth0.com"
               :client-id "test-client"
               :client-secret "test-secret"
               :audience "test-audience"}})))

(defn with-test-db
  "Test fixture that starts mount components including database"
  [f]
  ;; Override config with test configuration
  (let [test-config (load-test-config)]
    ;; Start mount with test config
    (mount/start-with-args test-config
                           #'cde.config/env
                           #'cde.db.core/*db*
                           #'cde.handler/app-routes)
    ;; Run tests
    (try
      (f)
      (finally
        ;; Stop mount components
        (mount/stop #'cde.handler/app-routes
                    #'cde.db.core/*db*
                    #'cde.config/env)))))

(defn with-rollback
  "Test fixture that wraps test in a database transaction and rolls it back"
  [f]
  (jdbc/with-transaction [tx db/*db* {:rollback-only true}]
    (binding [db/*db* tx]
      (f))))
