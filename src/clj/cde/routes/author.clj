(ns cde.routes.author
  "Routes for author-related operations"
  (:require
   [ring.util.http-response :as response]
   [cde.db.author :as author]
   [cde.middleware :as mw]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;; Specs
(s/def ::id
  (st/spec {:spec (s/and int? pos?)
            :name "Author ID"
            :description "The unique ID of the author."
            :json-schema/example 1}))

(s/def ::common_name string?)
(s/def ::other_name (s/nilable string?))
(s/def ::gender (s/nilable string?))
(s/def ::nationality (s/nilable string?))
(s/def ::nationality_details (s/nilable string?))
(s/def ::author_details (s/nilable string?))
(s/def ::limit (s/nilable int?))
(s/def ::offset (s/nilable int?))

(s/def ::list-parameters
  (s/keys :opt-un [::limit ::offset]))

(s/def ::create-request
  (s/keys :req-un [::common_name]
          :opt-un [::other_name
                   ::gender
                   ::nationality
                   ::nationality_details
                   ::author_details]))

(s/def ::update-request
  (s/keys :opt-un [::common_name
                   ::other_name
                   ::gender
                   ::nationality
                   ::nationality_details
                   ::author_details]))

(defn- with-defaults
  "Apply default values for limit and offset if not provided"
  [limit offset]
  [(if (nil? limit) 50 limit)
   (if (nil? offset) 0 offset)])

(defn author-routes []
  [["/authors"
    {:get {:summary "Get a list of all authors (with limit/offset)."
           :description ""
           :tags ["Authors"]
           :parameters {:query ::list-parameters}
           :responses {200 {:body ::specs/author-list-response}
                       400 {:body {:message string?}}}
           :handler (fn [{{{:keys [limit offset]} :query} :parameters}]
                      (let [[limit offset] (with-defaults limit offset)]
                        (try
                          (let [authors (author/get-authors limit offset)]
                            (response/ok (assoc authors
                                                :limit limit
                                                :offset offset)))
                          (catch Exception e
                            (response/not-found {:message (.getMessage e)})))))}}]

   ["/author/:id"
    {:get {:summary "Get details of a single author by id."
           :description ""
           :tags ["Authors"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/author-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [author (author/get-author id)]
                        (response/ok author)
                        (response/not-found {:message "Author not found"})))}

     :put {:summary "Update the details of a given author."
           :description ""
           :no-doc true
           :middleware [mw/check-auth0-jwt]
           :tags ["Authors" "Updating Existing Records"]
           :parameters {:path {:id ::id}
                        :body ::update-request}
           :responses {200 {:body ::specs/author-response}
                       400 {:body {:message string?}}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters
                          {:keys [body]} :parameters}]
                      (try
                        (if-let [author (author/update-author! id body)]
                          (response/ok author)
                          (response/not-found {:message "Author not found"}))
                        (catch Exception e
                          (response/bad-request {:message (.getMessage e)}))))}}]

   ["/author/:id/titles"
    {:get {:summary "Get a list of all titles by a single author (matched to that author's id)."
           :description ""
           :tags ["Authors"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/titles-by-author-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [titles (author/get-titles-by-author id)]
                        (response/ok titles)
                        (response/not-found {:message "No titles found for that author."})))}}]

   ["/author-nationalities"
    {:get {:summary "Get a list of all nationalities currently listed in our authors records."
           :description ""
           :no-doc true
           :tags ["Authors"]
           :responses {200 {:body ::specs/author-nationalities-response}
                       404 {:body {:message string?}}}
           :handler (fn [_]
                      (if-let [nationalities (author/get-nationalities)]
                        (response/ok nationalities)
                        (response/not-found {:message "No nationalities found"})))}}]

   ["/author-genders"
    {:get {:summary "Get a list of all genders currently listed in our authors records."
           :description ""
           :no-doc true
           :tags ["Authors"]
           :responses {200 {:body ::specs/author-genders-response}
                       404 {:body {:message string?}}}
           :handler (fn [_]
                      (if-let [genders (author/get-genders)]
                        (response/ok genders)
                        (response/not-found {:message "No genders found"})))}}]

   ["/create/author"
    {:post {:summary "Create a new author."
            :description ""
            :no-doc true
            :middleware [mw/check-auth0-jwt]
            :tags ["Authors" "Adding New Records"]
            :parameters {:body ::create-request}
            :responses {200 {:body {:message string? :id integer?}}
                        400 {:body {:message string?}}}
            :handler (fn [{:keys [parameters]}]
                       (let [body (:body parameters)]
                         (try
                           (let [id (author/create-author! body)]
                             (response/ok {:message "Author creation successful." :id id}))
                           (catch Exception e
                             (response/bad-request {:message (str "Author creation failed: " (.getMessage e))})))))}}]])
(ns cde.routes.author)
