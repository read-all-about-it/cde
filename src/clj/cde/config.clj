(ns cde.config
  "Configuration management using cprop.

   Loads configuration from multiple sources in order of precedence:
   1. dev-config.edn (development) or system config (production)
   2. Command-line arguments
   3. JVM system properties
   4. Environment variables

   The `env` state provides access to all configuration values throughout
   the application. Key configuration includes:
   - :database-url - PostgreSQL connection string
   - :port - HTTP server port
   - :nrepl-port - nREPL server port (development)
   - :auth0-details - Auth0 client configuration
   - :trove-api-keys - List of Trove API keys"
  (:require
   [cprop.core :refer [load-config]]
   [cprop.source :as source]
   [mount.core :refer [args defstate]]))

(defstate env
  "Mount state containing merged configuration from all sources.

  Configuration is loaded in order of precedence (later overrides earlier):
  1. Base config file (dev-config.edn or production equivalent)
  2. Command-line arguments via [[mount.core/args]]
  3. JVM system properties (-D flags)
  4. Environment variables

  Common configuration keys:
  - `:database-url` - PostgreSQL connection string
  - `:port` - HTTP server port (default 3000)
  - `:nrepl-port` - nREPL port for REPL connections
  - `:auth0-details` - Auth0 client configuration map
  - `:trove-api-keys` - Vector of Trove API keys for rotation"
  :start
  (load-config
   :merge
   [(args)
    (source/from-system-props)
    (source/from-env)]))
