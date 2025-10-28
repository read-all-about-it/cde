(ns cde.core
  "Application entry point and server lifecycle management.

   This namespace defines the main entry point for the TBC application,
   managing the lifecycle of the HTTP server and nREPL server using Mount.
   It handles command-line argument parsing, database migrations, and
   graceful shutdown.

   Key components:
   - `http-server`: Mount state for the Luminus HTTP server
   - `repl-server`: Mount state for the nREPL server (development)
   - `start-app`: Initialises all components and runs migrations
   - `-main`: CLI entry point with migration support"
  (:require
   [cde.handler :as handler]
   [cde.nrepl :as nrepl]
   [luminus.http-server :as http]
   [luminus-migrations.core :as migrations]
   [cde.config :refer [env]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [mount.core :as mount])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  "Command-line option specifications for tools.cli parsing.

  Supported options:
  - `-p`/`--port`: HTTP server port number (integer)"
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  "Mount state for the Luminus HTTP server.

  Starts the HTTP server with configuration from [[cde.config/env]].
  Port can be overridden via command-line `-p` option.

  See also: [[cde.handler/app]]"
  :start
  (http/start
   (-> env
       (assoc  :handler (handler/app))
       (update :port #(or (-> env :options :port) %))
       (select-keys [:handler :host :port :async?])))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  "Mount state for the nREPL server (development only).

  Only starts when `:nrepl-port` is configured in the environment.
  Configure `:nrepl-bind` to control the network interface binding.

  See also: [[cde.nrepl/start]], [[cde.nrepl/stop]]"
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))

(defn stop-app
  "Gracefully stops all Mount components and shuts down the agent thread pool.
   Called automatically on JVM shutdown via shutdown hook."
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app
  "Initialises the application with the given command-line arguments.

   Starts all Mount components (database, HTTP server, nREPL), runs any
   pending database migrations, and registers a shutdown hook for graceful
   termination.

   Parameters:
   - args: Command-line arguments (supports -p/--port for HTTP port)"
  [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (migrations/migrate ["migrate"] (select-keys env [:database-url]))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main
  "Main entry point for the application.

   Handles three modes of operation:
   1. Migration commands: init, migrate, rollback, etc.
   2. Normal startup: starts the full application
   3. Error: exits if database-url is not configured

   Parameters:
   - args: Command-line arguments passed to the JVM"
  [& args]
  (-> args
      (parse-opts cli-options)
      (mount/start-with-args #'cde.config/env))
  (cond
    (nil? (:database-url env))
    (do
      (log/error "Database configuration not found, :database-url environment variable must be set before running")
      (System/exit 1))
    (some #{"init"} args)
    (do
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))
    (migrations/migration? args)
    (do
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    :else
    (start-app args)))
