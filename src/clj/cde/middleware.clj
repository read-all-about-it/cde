(ns cde.middleware
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
   [ring.middleware.jwt :as jwt]
   [buddy.auth.backends.session :refer [session-backend]]))

(def jwks-url (str "https://" "read-all-about-it.au.auth0.com" "/.well-known/jwks.json"))

(defn wrap-auth0
  "Validates JWT tokens from Auth0"
  [handler]
  ;; In test mode, use mock JWT validation
  (if (get env :test-mode false)
    (fn [request]
      ;; Mock JWT validation for testing
      (if-let [auth-header (get-in request [:headers "authorization"])]
        (if (str/starts-with? auth-header "Bearer ")
          (let [token (str/replace auth-header "Bearer " "")]
            ;; Mock JWT claims for test tokens
            (handler (assoc request :jwt-claims
                            (if (= token "mock-test-token")
                              {"sub" "auth0|test-user"
                               "email" "test@example.com"
                               "name" "Test User"}
                             ;; Try to decode as test JWT
                              (try
                                (when (str/starts-with? token "test-")
                                  {"sub" (str "auth0|" token)
                                   "email" "test@example.com"
                                   "name" "Test User"})
                                (catch Exception _ nil))))))
          (handler request))
        (handler request)))
    ;; Production JWT validation
    (let [details {:alg :RS256
                   :jwk-endpoint jwks-url}]
      (jwt/wrap-jwt handler {:issuers {"https://read-all-about-it.au.auth0.com/" details
                                       :no-issuer details}
                             :reject-missing-token? false}))))

(defn test-middleware [handler]
  (fn [req]
    (let [auth-header (get-in req [:headers "authorization"])]
      (if (and auth-header (str/starts-with? auth-header "Bearer "))
        (let [token (str/replace auth-header "Bearer " "")]
          (println "token: " token))
        (println "no token"))
      (handler req))))

(defn print-auth0-cookie ;; TODO: remove this function
  "Prints the auth0 is.authenticated header."
  [handler]
  (fn [req]
    (let [client-id (get-in env [:auth0-details :client-id])
          headers (get-in req [:headers])
          cookie (get-in headers ["cookie"])]
      (if (and cookie (str/includes? cookie (str "auth0." client-id ".is.authenticated=true")))
        (println "auth0 is.authenticated header found")
        (println "auth0 is.authenticated header not found"))
      (handler req))))

(defn check-auth0-jwt
  "Validates Auth0 JWT token from Authorization header or returns 401.
   This properly validates the JWT signature and claims."
  [handler]
  (fn [req]
    (if-let [jwt-claims (:jwt-claims req)]
      (handler (assoc req :user-id (get jwt-claims "sub")
                      :user-email (get jwt-claims "email")))
      (error-page {:status 401
                   :title "Authentication required"
                   :message "Please provide a valid JWT token in the Authorization header."}))))

(defn check-auth0-cookie
  "DEPRECATED: Checks the auth0 is.authenticated cookie. This is insecure!
   Use check-auth0-jwt instead for proper token validation."
  [handler]
  (fn [req]
    (let [client-id (get-in env [:auth0-details :client-id])
          headers (get-in req [:headers])
          cookie (get-in headers ["cookie"])]
      (if (and cookie (str/includes? cookie (str "auth0." client-id ".is.authenticated=true")))
        (handler req)
        (error-page {:status 401
                     :title "You are not logged in."
                     :message "Please log in to continue."})))))

(defn extract-user-from-jwt
  "Extracts user information from validated JWT claims in the request.
   Returns nil if no valid JWT claims are present."
  [request]
  (when-let [claims (:jwt-claims request)]
    {:user-id (get claims "sub")
     :email (get claims "email")
     :name (get claims "name")}))

(defn wrap-https-redirect [handler]
  (fn [req]
    (if (= "http" (get-in req [:headers "x-forwarded-proto"]))
      {:status 301
       :headers {"Location" (str "https://" (:server-name req) (:uri req))
                 "Content-Type" "text/html"}
       :body "Redirecting to HTTPS..."}
      (handler req))))

(defn wrap-internal-error [handler]
  (let [error-result (fn [^Throwable t]
                       (log/error t (.getMessage t))
                       (error-page {:status 500
                                    :title "Something very bad has happened!"
                                    :message "We've dispatched a team of highly trained gnomes to take care of the problem."}))]
    (fn wrap-internal-error-fn
      ([req respond _]
       (handler req respond #(respond (error-result %))))
      ([req]
       (try
         (handler req)
         (catch Throwable t
           (error-result t)))))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
   handler
   {:error-response
    (error-page
     {:status 403
      :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn
      ([request]
         ;; disable wrap-formats for websockets
         ;; since they're not compatible with this middleware
       ((if (:websocket? request) handler wrapped) request))
      ([request respond raise]
       ((if (:websocket? request) handler wrapped) request respond raise)))))

(defn on-error [request response]
  (error-page
   {:status 403
    :title (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))
      wrap-internal-error))
