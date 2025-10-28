(ns cde.routes.newspaper
  "Newspaper entity API routes (CRUD operations).

  Provides endpoints for creating, reading, updating, and listing newspapers.
  Newspapers represent publication sources from the Trove archive.
  Create and update operations require JWT authentication.

  See also: [[cde.db.newspaper]] for database operations."
  (:require
   [ring.util.http-response :as response]
   [cde.db.newspaper :as newspaper]
   [cde.middleware :as mw]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;;;; Specs

(s/def ::id
  (st/spec {:spec (s/and int? pos?)
            :name "Newspaper ID"
            :description "The unique ID of the newspaper."
            :json-schema/example 1}))

(s/def ::title string?)
(s/def ::trove_newspaper_id int?)
(s/def ::common_title (s/nilable string?))
(s/def ::location (s/nilable string?))
(s/def ::details (s/nilable string?))
(s/def ::newspaper_type (s/nilable string?))
(s/def ::colony_state (s/nilable string?))
(s/def ::start_year (s/nilable int?))
(s/def ::end_year (s/nilable int?))
(s/def ::start_date (s/nilable string?))
(s/def ::end_date (s/nilable string?))
(s/def ::issn (s/nilable string?))
(s/def ::added_by (s/nilable int?))
(s/def ::limit (s/nilable int?))
(s/def ::offset (s/nilable int?))

(s/def ::list-parameters
  (s/keys :opt-un [::limit ::offset]))

(s/def ::create-request
  (s/keys :req-un [::title ::trove_newspaper_id]
          :opt-un [::common_title
                   ::location
                   ::details
                   ::newspaper_type
                   ::colony_state
                   ::start_year
                   ::end_year
                   ::start_date
                   ::end_date
                   ::issn
                   ::added_by]))

(s/def ::update-request
  (s/keys :opt-un [::title
                   ::common_title
                   ::location
                   ::details
                   ::newspaper_type
                   ::colony_state
                   ::start_year
                   ::end_year
                   ::start_date
                   ::end_date
                   ::issn]))

(defn- ^:no-doc with-defaults
  "Applies default pagination values.

  Arguments:
  - `limit` - Max results (default 50)
  - `offset` - Pagination offset (default 0)

  Returns: Vector of [limit offset] with defaults applied."
  [limit offset]
  [(if (nil? limit) 50 limit)
   (if (nil? offset) 0 offset)])

(defn newspaper-routes
  "Returns newspaper-related route definitions.

  Routes:
  - `GET /newspapers` - List newspapers (paginated)
  - `GET /newspaper/:id` - Get newspaper by ID
  - `PUT /newspaper/:id` - Update newspaper (auth required)
  - `GET /newspaper/:id/titles` - Get titles in newspaper
  - `POST /create/newspaper` - Create newspaper (auth required)

  Returns: Vector of Reitit route definitions."
  []
  [["/newspapers"
    {:get {:summary "Get a list of all newspapers (with limit/offset)."
           :description ""
           :tags ["Newspapers"]
           :parameters {:query ::list-parameters}
           :responses {200 {:body ::specs/newspaper-list-response}
                       400 {:body {:message string?}}}
           :handler (fn [{{{:keys [limit offset]} :query} :parameters}]
                      (let [[limit offset] (with-defaults limit offset)]
                        (try
                          (let [newspapers (newspaper/get-newspapers limit offset)]
                            (response/ok (assoc newspapers
                                                :limit limit
                                                :offset offset)))
                          (catch Exception e
                            (response/not-found {:message (.getMessage e)})))))}}]

   ["/newspaper/:id"
    {:get {:summary "Get details of a single newspaper."
           :description ""
           :tags ["Newspapers"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/newspaper-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (try
                        (let [newspaper (newspaper/get-newspaper id)]
                          (response/ok newspaper))
                        (catch Exception e
                          (response/not-found {:message "Newspaper not found"}))))}

     :put {:summary "Update the details of a given newspaper."
           :description ""
           :no-doc true
           :middleware [mw/check-auth0-jwt]
           :tags ["Newspapers" "Updating Existing Records"]
           :parameters {:path {:id ::id}
                        :body ::update-request}
           :responses {200 {:body ::specs/newspaper-response}
                       400 {:body {:message string?}}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters
                          {:keys [body]} :parameters}]
                      (try
                        (if-let [newspaper (newspaper/update-newspaper! id body)]
                          (response/ok newspaper)
                          (response/not-found {:message "Newspaper not found"}))
                        (catch Exception e
                          (response/bad-request {:message (.getMessage e)}))))}}]

   ["/newspaper/:id/titles"
    {:get {:summary "Get a list of all titles published in a given newspaper."
           :description ""
           :tags ["Newspapers"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/titles-in-newspaper-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [titles (newspaper/get-titles-in-newspaper id)]
                        (response/ok titles)
                        (response/not-found {:message "No titles found"})))}}]

   ["/create/newspaper"
    {:post {:summary "Create a new newspaper."
            :description ""
            :no-doc true
            :middleware [mw/check-auth0-jwt]
            :tags ["Newspapers" "Adding New Records"]
            :parameters {:body ::create-request}
            :responses {200 {:message string? :id integer?}
                        400 {:body {:message string?}}}
            :handler (fn [{:keys [parameters]}]
                       (let [body (:body parameters)]
                         (try
                           (let [id (newspaper/create-newspaper! body)]
                             (response/ok {:message "Newspaper creation successful."
                                           :id id}))
                           (catch Exception e
                             (response/bad-request {:message (str "Newspaper creation failed: " (.getMessage e))})))))}}]])
