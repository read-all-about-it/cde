(ns cde.routes.auth
  "Authentication and user management routes"
  (:require
   [ring.util.http-response :as response]
   [cde.db.user :as user]
   [cde.middleware :as mw]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;; Specs
(def ^:private email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

(s/def ::email (s/and string? #(re-matches email-regex %)))

(s/def ::user/request-parameters
  (s/keys :req-un [::email]))

(s/def ::user/id
  (st/spec {:spec (s/and int? pos?)
           :name "User ID"
           :description "The unique ID of the user."
           :json-schema/example 1}))

(defn auth-routes []
  [["/test"
    {:get {:summary "A test endpoint."
           :middleware [mw/check-auth0-jwt]
           :description ""
           :no-doc true
           :tags ["Test"]
           :responses {200 {:body {:message string?
                                   :now string?}}
                      400 {:body {:message string?}}}
           :handler (fn [request-map]
                     (response/ok {:message "Hello, world!"
                                  :now (str (java.util.Date.))}))}}]

   ["/user"
    {:get {:summary "Get a user/email map given an email (passed in query params), creating a user record if necessary."
           :description ""
           :no-doc true
           :tags ["User"]
           :parameters {:query ::user/request-parameters}
           :responses {200 {:body {:id ::user/id
                                   :email ::email}}
                      400 {:body {:message string?}}}
           :handler (fn [request-map]
                     (let [query (get-in request-map [:parameters :query])
                           email (:email query)]
                       (try
                         (let [user (user/get-or-create-user! email)]
                           (response/ok {:id (:id user) :email (:email user)}))
                         (catch Exception e
                           (response/bad-request {:message (.getMessage e)})))))}}]])
(ns cde.routes.auth)