(ns cde.routes.title
  "Routes for title-related operations"
  (:require
   [ring.util.http-response :as response]
   [cde.db.title :as title]
   [cde.db.chapter :as chapter]
   [cde.middleware :as mw]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;; Specs
(s/def ::id
  (st/spec {:spec (s/and int? pos?)
            :name "Title ID"
            :description "The unique ID of the title."
            :json-schema/example 1}))

(s/def ::author_id int?)
(s/def ::newspaper_table_id int?)
(s/def ::publication_title (s/nilable string?))
(s/def ::attributed_author_name (s/nilable string?))
(s/def ::author_of (s/nilable string?))
(s/def ::additional_info (s/nilable string?))
(s/def ::inscribed_author_nationality (s/nilable string?))
(s/def ::inscribed_author_gender (s/nilable string?))
(s/def ::information_source (s/nilable string?))
(s/def ::length (s/nilable int?))
(s/def ::trove_source (s/nilable string?))
(s/def ::also_published (s/nilable string?))
(s/def ::name_category (s/nilable string?))
(s/def ::curated_dataset (s/nilable boolean?))
(s/def ::limit (s/nilable int?))
(s/def ::offset (s/nilable int?))

(s/def ::list-parameters
  (s/keys :opt-un [::limit ::offset]))

(s/def ::create-request
  (s/keys :req-un [::author_id ::newspaper_table_id]
          :opt-un [::publication_title
                   ::attributed_author_name
                   ::author_of
                   ::additional_info
                   ::inscribed_author_nationality
                   ::inscribed_author_gender
                   ::information_source
                   ::length
                   ::trove_source
                   ::also_published
                   ::name_category
                   ::curated_dataset]))

(s/def ::update-request
  (s/keys :opt-un [::author_id
                   ::newspaper_table_id
                   ::publication_title
                   ::attributed_author_name
                   ::author_of
                   ::additional_info
                   ::inscribed_author_nationality
                   ::inscribed_author_gender
                   ::information_source
                   ::length
                   ::trove_source
                   ::also_published
                   ::name_category
                   ::curated_dataset]))

(defn- with-defaults
  "Apply default values for limit and offset if not provided"
  [limit offset]
  [(if (nil? limit) 50 limit)
   (if (nil? offset) 0 offset)])

(defn title-routes []
  [["/titles"
    {:get {:summary "Get a list of all titles (with limit/offset)."
           :description ""
           :tags ["Titles"]
           :parameters {:query ::list-parameters}
           :responses {200 {:body ::specs/title-list-response}
                       400 {:body {:message string?}}}
           :handler (fn [{{{:keys [limit offset]} :query} :parameters}]
                      (let [[limit offset] (with-defaults limit offset)]
                        (try
                          (let [titles (title/get-titles limit offset)]
                            (response/ok (assoc titles
                                                :limit limit
                                                :offset offset)))
                          (catch Exception e
                            (response/not-found {:message (.getMessage e)})))))}}]

   ["/title/:id"
    {:get {:summary "Get details of a single title by id."
           :description ""
           :tags ["Titles"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/single-title-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [title (title/get-title id true)]
                        (response/ok title)
                        (response/not-found {:message "Title not found"})))}

     :put {:summary "Update the details of a given title."
           :description ""
           :no-doc true
           :middleware [mw/check-auth0-jwt]
           :tags ["Titles" "Updating Existing Records"]
           :parameters {:path {:id ::id}
                        :body ::update-request}
           :responses {200 {:body ::specs/single-title-response}
                       400 {:body {:message string?}}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters
                          {:keys [body]} :parameters}]
                      (try
                        (if-let [title (title/update-title! id body)]
                          (response/ok title)
                          (response/not-found {:message "Title not found"}))
                        (catch Exception e
                          (response/bad-request {:message (.getMessage e)}))))}}]

   ["/title/:id/chapters"
    {:get {:summary "Get a list of all chapters in a given title."
           :description ""
           :tags ["Titles"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/chapters-within-title-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [chapters (chapter/get-chapters-in-title id)]
                        (response/ok chapters)
                        (response/not-found {:message "No chapters found"})))}}]

   ["/create/title"
    {:post {:summary "Create a new title."
            :description ""
            :no-doc true
            :middleware [mw/check-auth0-jwt]
            :tags ["Titles" "Adding New Records"]
            :parameters {:body ::create-request}
            :responses {200 {:body {:message string?
                                    :id ::id}}
                        400 {:body {:message string?}}}
            :handler (fn [{:keys [parameters]}]
                       (let [body (:body parameters)]
                         (try
                           (let [id (title/create-title! body)]
                             (response/ok {:message "Title creation successful." :id id}))
                           (catch Exception e
                             (response/bad-request {:message (str "Title creation failed: " (.getMessage e))})))))}}]])
(ns cde.routes.title)
