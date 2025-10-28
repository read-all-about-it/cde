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

(defn load-test-config
  "Load test configuration from test-config.edn or dev-config.edn.

  Tries sources in order:
  1. test-config.edn (preferred for isolated test database)
  2. dev-config.edn (fallback for local development)
  3. Minimal hardcoded config (last resort)

  Uses same key structure as dev-config.edn:
  - `:auth0-details` for Auth0 configuration
  - `:trove-api-keys` for Trove API keys (vector)"
  []
  (let [test-config (io/resource "test-config.edn")
        dev-config (io/file "dev-config.edn")]
    (cond
      ;; Prefer test-config.edn if it exists
      test-config
      (edn/read-string (slurp test-config))

      ;; Fall back to dev-config.edn if available
      (.exists dev-config)
      (edn/read-string (slurp dev-config))

      ;; Last resort: minimal config (tests will skip DB operations)
      :else
      {:auth0-details {:domain "test.auth0.com"
                       :client-id "test-client"
                       :client-secret "test-secret"
                       :audience "test-audience"}
       :trove-api-keys ["test-trove-key"]
       :test-mode true})))

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

(defn db-available?
  "Returns true if the database connection is properly initialized.

  Checks that `*db*` is not in NotStartedState and that a database URL
  is configured in the environment."
  []
  (and (not (instance? mount.core.NotStartedState db/*db*))
       (:database-url env)))

(defn with-rollback
  "Test fixture that wraps test in a database transaction and rolls it back.

  If the database is not available (no URL configured or mount not started),
  the test function is still called but without transaction wrapping. Tests
  should use [[db-available?]] to skip database operations in this case."
  [f]
  (if (db-available?)
    (jdbc/with-transaction [tx db/*db* {:rollback-only true}]
      (binding [db/*db* tx]
        (f)))
    (f)))

(defn with-db
  "Test fixture that starts only database-related mount components.

  Use this for database module tests that don't need the full handler/routes.
  Loads configuration from test-config.edn or dev-config.edn via [[load-test-config]].
  Runs migrations if database URL is configured. Pairs with [[with-rollback]]
  for per-test transaction isolation.

  Example:
    (use-fixtures :once with-db)
    (use-fixtures :each with-rollback)"
  [f]
  (let [test-config (load-test-config)]
    (mount/start-with-args test-config
                           #'cde.config/env
                           #'cde.db.core/*db*)
    (try
      ;; Run migrations if database URL is configured
      (when-let [db-url (:database-url env)]
        (try
          ((requiring-resolve 'luminus-migrations.core/migrate)
           ["migrate"]
           {:database-url db-url})
          (catch Exception e
            (println "Warning: Could not run migrations:" (.getMessage e)))))
      (f)
      (finally
        (mount/stop #'cde.db.core/*db*
                    #'cde.config/env)))))
