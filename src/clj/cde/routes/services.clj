(ns cde.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [cde.middleware.formats :as formats]
   [clojure.java.io :as io]
   [cde.auth :as auth]
   [cde.search :as search]
   [ring.util.http-response :as response]
   [clojure.spec.alpha :as s]))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/register"
    {:post {:parameters {:body {:username string? :email string? :password string? :confirm string?}}
            :responses {200 {:body {:message string?}}
                        400 {:body {:message string?}}
                        409 {:body {:message string?}}}
            :handler (fn [{{{:keys [username email password confirm]} :body} :parameters}]
                       (if-not (= password confirm)
                         (response/bad-request {:message "Password and Confirm do not match."})
                         (try
                           (auth/create-user! username email password)
                           (response/ok {:message "User registration successful. Please log in."})
                           (catch clojure.lang.ExceptionInfo e
                             (cond
                               (= (:cde/error-id (ex-data e)) :auth/duplicate-user-username)
                               (response/conflict {:message "Registration failed! A user with that username already exists!"})
                               (= (:cde/error-id (ex-data e)) :auth/duplicate-user-email)
                               (response/conflict {:message "Registration failed! A user with that email already exists!"}))
                             :else (throw e)))))}}]

   ["/login"
    {:post {:parameters {:body {:email string?, :password string?}}
            :responses {200 {:body
                             {:identity {:email string? :created_at any?}}}
                        401 {:body {:message string?}}}
            :handler (fn [{{{:keys [email password]} :body} :parameters
                           session :session}]
                       (if-some [user (auth/authenticate-user email password)]
                         (-> (response/ok
                              {:identity user})
                             (assoc :session (assoc session :identity user)))
                         (response/unauthorized
                          {:message "Invalid email or password"})))}}]

   ["/logout"
    {:post {:handler (fn [_] (-> (response/ok)
                                 (assoc :session nil)))}}]

   ["/search"
    {:get {:parameters {:query {:query map?, :limit (s/nilable int?), :offset (s/nilable int?)}}
           :responses {200 {:body {:results vector?}}
                       400 {:body {:message string?}}}
           :handler (fn [{{{:keys [query limit offset]} :query} :parameters}]
                      (if query
                        (let [results (search/search-titles query limit offset)]
                          (response/ok {:results results}))
                        (response/bad-request {:message "Invalid query"})))}}]])