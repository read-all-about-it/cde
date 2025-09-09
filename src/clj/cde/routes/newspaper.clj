(ns cde.routes.newspaper
  "Routes for newspaper-related operations"
  (:require
   [ring.util.http-response :as response]
   [cde.db.newspaper :as newspaper]
   [cde.middleware :as mw]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;; Specs
(s/def ::id
  (st/spec {:spec (s/and int? pos?)
            :name "Newspaper ID"
            :description "The unique ID of the newspaper."
            :json-schema/example 1}))

(s/def ::newspaper_title (s/nilable string?))
(s/def ::location (s/nilable string?))
(s/def ::details (s/nilable string?))
(s/def ::newspaper_type (s/nilable string?))
(s/def ::colony_state (s/nilable string?))
(s/def ::limit (s/nilable int?))
(s/def ::offset (s/nilable int?))

(s/def ::list-parameters
  (s/keys :opt-un [::limit ::offset]))

(s/def ::create-request
  (s/keys :opt-un [::newspaper_title
                   ::location
                   ::details
                   ::newspaper_type
                   ::colony_state]))

(defn- with-defaults
  "Apply default values for limit and offset if not provided"
  [limit offset]
  [(if (nil? limit) 50 limit)
   (if (nil? offset) 0 offset)])

(defn newspaper-routes []
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
                      (if-let [newspaper (newspaper/get-newspaper id)]
                        (response/ok newspaper)
                        (response/not-found {:message "Newspaper not found"})))}}]

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
(ns cde.routes.newspaper)
