(ns cde.ajax
  "HTTP client configuration and request interceptors.

  Provides AJAX utilities for communicating with the backend API:
  - Automatic CSRF token injection for local requests
  - Bearer token authentication via Auth0 access tokens
  - Transit+JSON serialization for request/response formatting

  Uses cljs-ajax with custom interceptors that are installed at
  application startup via [[load-interceptors!]].

  See also: [[cde.events]] for HTTP effect handlers."
  (:require
   [ajax.core :as ajax]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn local-uri?
  "Checks if a request URI is local (relative) vs external (absolute).

  Local URIs don't have a protocol prefix (e.g., `/api/v1/titles`).
  External URIs start with a protocol (e.g., `https://trove.nla.gov.au`).

  Arguments:
  - `request` - map containing `:uri` key

  Returns: boolean, true if URI is local."
  [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn get-auth-token
  "Retrieves the Auth0 access token from re-frame application state.

  Synchronously dereferences the `:auth/tokens` subscription to get
  the current JWT access token for API authentication.

  Returns: string access token, or nil if user is not authenticated."
  []
  (let [db @(rf/subscribe [:auth/tokens])]
    db))

(defn default-headers
  "Adds default headers to local HTTP requests.

  For local (relative) URIs, injects:
  - `x-csrf-token` - CSRF protection token from `js/csrfToken`
  - `Authorization` - Bearer token with Auth0 access token (if authenticated)

  External requests are passed through unchanged.

  Arguments:
  - `request` - cljs-ajax request map with `:uri` and `:headers`

  Returns: request map with merged headers for local requests."
  [request]
  (if (local-uri? request)
    (let [token (get-auth-token)
          base-headers {"x-csrf-token" js/csrfToken}
          headers (if token
                    (assoc base-headers "Authorization" (str "Bearer " token))
                    base-headers)]
      (-> request
          (update :headers #(merge headers %))))
    request))

;;;; Transit Serialization

(defn as-transit
  "Wraps request options with Transit+JSON serialization configuration.

  Configures both request and response formats to use Transit with
  time serialization handlers from luminus-transit for proper
  date/time handling.

  Arguments:
  - `opts` - cljs-ajax request options map

  Returns: opts merged with `:format` and `:response-format` for Transit."
  [opts]
  (merge {:format          (ajax/transit-request-format
                            {:writer (transit/writer :json time/time-serialization-handlers)})
          :response-format (ajax/transit-response-format
                            {:reader (transit/reader :json time/time-deserialization-handlers)})}
         opts))

;;;; Interceptor Setup

(defn load-interceptors!
  "Installs default AJAX interceptors for the application.

  Adds the [[default-headers]] interceptor to cljs-ajax's global
  interceptor chain. This ensures all requests automatically include
  CSRF tokens and authentication headers.

  Should be called once during application initialization.
  See [[cde.core/init!]]."
  []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))
