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

(defn wrap-auth0 [handler]
  (let [details {:alg :RS256
                 :jwk-endpoint jwks-url}]
    (jwt/wrap-jwt handler {:issuers {"https://read-all-about-it.au.auth0.com/" details
                                     :no-issuer details}
                           :reject-missing-token? false})))

(defn test-middleware [handler]
    (fn [req]
      (let [auth-header (get-in req [:headers "authorization"])]
        (if (and auth-header (str/starts-with? auth-header "Bearer "))
          (let [token (str/replace auth-header "Bearer " "")]
            (println "token: " token))
          (println "no token"))
        (handler req))))

(defn print-auth0-header
  "Prints the auth0 is.authenticated header"
  [handler]
  (fn [req]
    (let [headers (get-in req [:headers])]
      (println headers)
      (handler req))))


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