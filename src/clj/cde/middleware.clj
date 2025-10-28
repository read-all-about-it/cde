(ns cde.middleware
  "Request/response middleware stack and authentication.

   This namespace defines the middleware pipeline for all HTTP requests,
   including JWT authentication, error handling, content negotiation,
   and security features.

   Key middleware:
   - `wrap-auth0`: Validates JWT tokens from Auth0 (RS256)
   - `check-auth0-jwt`: Enforces authentication, returns 401 if invalid
   - `wrap-base`: Composes all middleware into the final stack

   Authentication flow:
   1. `wrap-auth0` extracts and validates JWT from Authorization header
   2. Valid claims are attached to request as :jwt-claims
   3. `check-auth0-jwt` (on protected routes) enforces authentication
   4. User info extracted from claims for downstream handlers"
  (:require
   [cde.env :refer [defaults]]
   [clojure.tools.logging :as log]
   [cde.layout :refer [error-page]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [cde.middleware.formats :as formats]
   [muuntaja.middleware :refer [wrap-format wrap-params]]
   [cde.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth :refer [authenticated?]]
   [clojure.string :as str]
   [buddy.auth.backends.session :refer [session-backend]]
   [cde.jwt-auth :as jwt-auth]))

(defn wrap-auth0
  "Validates JWT tokens from Auth0 and attaches claims to the request.

  Extracts the Bearer token from the Authorization header and verifies
  it using RS256 signature validation via [[cde.jwt-auth/verify-jwt]].
  Valid claims are attached to the request as `:jwt-claims`.

  In test mode (`:test-mode` in env), uses mock validation for tokens
  prefixed with `test-` or equal to `mock-test-token`.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function."
  [handler]
  (fn [request]
    ;; Check test mode at runtime, not at middleware creation time
    (if (:test-mode env)
      ;; Mock JWT validation for testing
      (if-let [auth-header (get-in request [:headers "authorization"])]
        (if (str/starts-with? auth-header "Bearer ")
          (let [token (str/trim (str/replace auth-header "Bearer " ""))]
            ;; Mock JWT claims for test tokens
            (handler (assoc request :jwt-claims
                            (cond
                              (= token "mock-test-token")
                              {"sub" "auth0|test-user"
                               "email" "test@example.com"
                               "name" "Test User"}

                              (str/starts-with? token "test-")
                              {"sub" (str "auth0|" token)
                               "email" "test@example.com"
                               "name" "Test User"}

                              :else nil))))
          (handler request))
        (handler request))
      ;; Production JWT validation with RS256 signature verification
      (let [auth-header (get-in request [:headers "authorization"])]
        (if (and auth-header (str/starts-with? auth-header "Bearer "))
          (let [token (str/trim (subs auth-header 7))
                claims (jwt-auth/verify-jwt token)]
            (handler (assoc request :jwt-claims claims)))
          (handler request))))))

(defn check-auth0-jwt
  "Enforces authentication by requiring valid JWT claims on the request.

  If `:jwt-claims` is present (set by [[wrap-auth0]]), extracts user ID
  and email from claims and attaches them as `:user-id` and `:user-email`.
  Otherwise, returns a 401 Unauthorized error page.

  Use this middleware on protected routes that require authentication.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function."
  [handler]
  (fn [req]
    (if-let [jwt-claims (:jwt-claims req)]
      (handler (assoc req :user-id (get jwt-claims "sub")
                      :user-email (get jwt-claims "email")))
      (error-page {:status 401
                   :title "Authentication required"
                   :message "Please provide a valid JWT token in the Authorization header."}))))

(defn wrap-csrf
  "Wraps handler with CSRF protection using anti-forgery tokens.

  Validates the anti-forgery token on state-changing requests (POST, PUT, etc.).
  Returns a 403 error page if the token is invalid or missing.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function with CSRF protection."
  [handler]
  (wrap-anti-forgery
   handler
   {:error-response
    (error-page
     {:status 403
      :title "403 - Invalid anti-forgery token"})}))

(defn wrap-formats
  "Wraps handler with content negotiation and parameter parsing.

  Uses Muuntaja to handle request/response encoding for Transit, JSON,
  EDN, and other formats. Automatically parses request parameters.
  Bypasses formatting for WebSocket upgrade requests.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function with format negotiation.

  See also: [[cde.middleware.formats/instance]]"
  [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ((if (:websocket? request) handler wrapped) request))))

(defn- ^:no-doc on-error
  "Error handler for authentication failures.

  Arguments:
  - `request` - Ring request map
  - `value` - Authentication failure value (unused)

  Returns: 403 Forbidden error page response."
  [request value]
  (error-page
   {:status 403
    :title "403 - Forbidden"
    :message (str "Access to " (:uri request) " is not authorized.")}))

(defn wrap-restricted
  "Restricts access to authenticated users only.

  Uses buddy-auth to check if the request has an authenticated identity.
  Returns 403 Forbidden for unauthenticated requests.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function with authentication enforcement."
  [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-auth
  "Applies session-based authentication backend to the handler.

  Wraps the handler with buddy-auth authentication and authorization
  middleware using the session backend.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function with authentication support."
  [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-https-redirect
  "Redirects HTTP requests to HTTPS when behind a reverse proxy.

  Checks the `X-Forwarded-Proto` header to detect if the original
  request was HTTP. If so, returns a 301 redirect to the HTTPS URL.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function with HTTPS redirect support."
  [handler]
  (fn [req]
    (if (= "http" (get-in req [:headers "x-forwarded-proto"]))
      {:status 301
       :headers {"Location" (str "https://" (:server-name req) (:uri req))
                 "Content-Type" "text/html"}
       :body "Redirecting to HTTPS..."}
      (handler req))))

(defn wrap-internal-error
  "Catches all unhandled exceptions and returns a 500 error page.

  Logs the full exception with stack trace for debugging purposes,
  then returns a generic 500 error page to the client.

  Arguments:
  - `handler` - Next Ring handler in the middleware chain

  Returns: Ring handler function with exception handling."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "500 - Something very bad has happened!"
                     :message "We've dispatched a team to fix the issue."})))))

(defn wrap-base
  "Composes the complete middleware stack for the application.

   Middleware applied (outermost first):
   1. Internal error handling (500 pages)
   2. Site defaults (security headers, static resources)
   3. Session authentication backend
   4. JWT token validation (Auth0)
   5. Environment-specific middleware (dev/prod)"
  [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth0
      wrap-auth
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (dissoc :session)))
      wrap-internal-error))
