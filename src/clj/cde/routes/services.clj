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
   [cde.newspaper :as newspaper]
   [cde.article :as article]
   [ring.util.http-response :as response]
   [spec-tools.core :as st]
   [clojure.spec.alpha :as s]))

(s/def ::trove-newspaper-id int?)
(s/def ::title string?)
(s/def ::common-title (s/nilable string?))
(s/def ::location (s/nilable string?))
(s/def ::start-year (s/nilable int?))
(s/def ::end-year (s/nilable int?))
(s/def ::details (s/nilable string?))
(s/def ::newspaper-type (s/nilable string?))
(s/def ::colony-state (s/nilable string?))

;; start-date and end-date must be in the format "yyyy-mm-dd" (for postgres)
(defn- date? [s]
  (if (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    true
    false))

(s/def ::start-date (s/nilable string?))
(s/def ::end-date (s/nilable string?))

(s/def ::issn (s/nilable string?))
(s/def ::user-id int?)

(s/def ::create-newspaper-request
  (s/keys :req-un [::trove-newspaper-id
                   ::title]
          :opt-un [::common-title
                   ::location
                   ::start-year
                   ::end-year
                   ::details
                   ::newspaper-type
                   ::colony-state
                   ::start-date
                   ::end-date
                   ::issn]))


(s/def ::profile-response map?)



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
                       (println "session: " session)
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
    {:get {:parameters {:query {:common-title (s/nilable string?),
                                :newspaper-title (s/nilable string?),
                                :chapter-text (s/nilable string?),
                                :author (s/nilable string?),
                                :nationality (s/nilable string?),
                                :gender (s/nilable string?),
                                :length (s/nilable int?),
                                :limit (s/nilable int?),
                                :offset (s/nilable int?)}}
           :responses {200 {:body {:results vector?}}
                       400 {:body {:message string?}}}
           :handler (fn [{:keys [parameters]}]
                      (let [params (:query parameters)
                            cleaned-params (into {} (filter #(not (empty? (second %))) params))
                            {:keys [common-title newspaper-title chapter-text author nationality gender length limit offset]} cleaned-params]
                        (if (nil? (some identity [common-title newspaper-title chapter-text author nationality gender length]))
                          {:status 400
                           :body {:message "At least one parameter must be non-nil"}}
                          (let [results (search/search-titles cleaned-params)]
                            {:status 200
                             :body {:results results}}))))}}]

   ["/profile/:id" {:get {:parameters {:path {:id ::user-id}}
                          :responses {200 {:body ::profile-response}
                                      404 {:body {:message string?}}}
                          :handler (fn [{{{:keys [id]} :path} :parameters}]
                                     (if-let [user (auth/get-user-profile id)]
                                       (response/ok user)
                                       (response/not-found {:message "User profile not found"})))}}]
   ["/create/newspaper"
    {:post {:parameters {:body ::create-newspaper-request}
            :responses {200 {:body {:message string?}}
                        400 {:body {:message string?}}}
            :handler (fn [{:keys [parameters]}]
                       (let [body (:body parameters)]
                         (try
                           (newspaper/create-newspaper! body)
                           (response/ok {:message "Newspaper creation successful."})
                           (catch Exception e
                             (response/bad-request {:message (str "Newspaper creation failed: " (.getMessage e))})))))}}]
   
    ["/create/article"
     {:post {:parameters {:body ::create-article-request}
             :responses {200 {:body {:message string?}}
                         400 {:body {:message string?}}}
             :handler (fn [{:keys [parameters]}]
                        (let [body (:body parameters)]
                          (try
                            (article/create-article! body)
                            (response/ok {:message "Article creation successful."})
                            (catch Exception e
                              (response/bad-request {:message (str "Article creation failed: " (.getMessage e))})))))}}]
   ])