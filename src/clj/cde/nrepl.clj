(ns cde.nrepl
  "nREPL server configuration for interactive development.

   Provides start/stop functions for an embedded nREPL server, allowing
   REPL-driven development by connecting from an editor (Emacs/CIDER,
   IntelliJ/Cursive, VS Code/Calva).

   The server is configured via :nrepl-port and :nrepl-bind in the
   environment configuration. Only starts in development when configured."
  (:require
   [nrepl.server :as nrepl]
   [clojure.tools.logging :as log]))

(defn start
  "Starts an nREPL server for interactive development.

  All options are forwarded to `nrepl.server/start-server`.

  Arguments:
  - `opts` - Configuration map with keys:
    - `:port` - Port number to listen on (required)
    - `:bind` - Network interface to bind to (default: localhost)
    - `:transport-fn` - Custom transport function
    - `:handler` - Custom nREPL handler
    - `:ack-port` - Port to acknowledge startup
    - `:greeting-fn` - Function to generate greeting message

  Returns: nREPL server instance.

  Throws: Re-throws any exception after logging."
  [{:keys [port bind transport-fn handler ack-port greeting-fn]}]
  (try
    (log/info "starting nREPL server on port" port)
    (nrepl/start-server :port port
                        :bind bind
                        :transport-fn transport-fn
                        :handler handler
                        :ack-port ack-port
                        :greeting-fn greeting-fn)

    (catch Throwable t
      (log/error t "failed to start nREPL")
      (throw t))))

(defn stop
  "Stops the nREPL server and logs the shutdown.

  Arguments:
  - `server` - nREPL server instance returned by [[start]]

  Returns: nil"
  [server]
  (nrepl/stop-server server)
  (log/info "nREPL server stopped"))
