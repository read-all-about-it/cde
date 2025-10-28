(ns cde.routes.chapter
  "Chapter entity API routes (CRUD operations).

  Provides endpoints for creating, reading, updating, and listing chapters.
  Chapters represent individual instalments of serialised fiction,
  corresponding to newspaper articles in the Trove archive.
  Create and update operations require JWT authentication.

  See also: [[cde.db.chapter]] for database operations."
  (:require
   [ring.util.http-response :as response]
   [cde.db.chapter :as chapter]
   [cde.middleware :as mw]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;;;; Specs

(s/def ::id
  (st/spec {:spec (s/and int? pos?)
            :name "Chapter ID"
            :description "The unique ID of the chapter."
            :json-schema/example 1}))

(s/def ::title_id int?)
(s/def ::trove_article_id int?)
(s/def ::chapter_number (s/nilable string?))
(s/def ::chapter_title (s/nilable string?))
(s/def ::article_url (s/nilable string?))
(s/def ::page_references (s/nilable int?))
(s/def ::page_url (s/nilable string?))
(s/def ::word_count (s/nilable int?))
(s/def ::illustrated (s/nilable boolean?))
(s/def ::page_sequence (s/nilable string?))
(s/def ::chapter_html (s/nilable string?))
(s/def ::chapter_text (s/nilable string?))
(s/def ::text_title (s/nilable string?))
(s/def ::export_title (s/nilable string?))
(s/def ::limit (s/nilable int?))
(s/def ::offset (s/nilable int?))

(s/def ::list-parameters
  (s/keys :opt-un [::limit ::offset]))

(s/def ::create-request
  (s/keys :req-un [::title_id ::trove_article_id]
          :opt-un [::chapter_number
                   ::chapter_title
                   ::article_url
                   ::page_references
                   ::page_url
                   ::word_count
                   ::illustrated
                   ::page_sequence
                   ::chapter_html
                   ::chapter_text
                   ::text_title
                   ::export_title]))

(s/def ::update-request
  (s/keys :opt-un [::title_id
                   ::trove_article_id
                   ::chapter_number
                   ::chapter_title
                   ::article_url
                   ::page_references
                   ::page_url
                   ::word_count
                   ::illustrated
                   ::page_sequence
                   ::chapter_html
                   ::chapter_text
                   ::text_title
                   ::export_title]))

(defn- ^:no-doc with-defaults
  "Applies default pagination values.

  Arguments:
  - `limit` - Max results (default 50)
  - `offset` - Pagination offset (default 0)

  Returns: Vector of [limit offset] with defaults applied."
  [limit offset]
  [(if (nil? limit) 50 limit)
   (if (nil? offset) 0 offset)])

(defn chapter-routes
  "Returns chapter-related route definitions.

  Routes:
  - `GET /chapters` - List chapters (paginated)
  - `GET /chapter/:id` - Get chapter by ID
  - `PUT /chapter/:id` - Update chapter (auth required)
  - `POST /create/chapter` - Create chapter (auth required)

  Returns: Vector of Reitit route definitions."
  []
  [["/chapters"
    {:get {:summary "Get a list of all chapters (with limit/offset)."
           :description ""
           :tags ["Chapters"]
           :parameters {:query ::list-parameters}
           :responses {200 {:body ::specs/chapter-list-response}
                       400 {:body {:message string?}}}
           :handler (fn [{{{:keys [limit offset]} :query} :parameters}]
                      (let [[limit offset] (with-defaults limit offset)]
                        (try
                          (let [chapters (chapter/get-chapters limit offset)]
                            (response/ok (assoc chapters
                                                :limit limit
                                                :offset offset)))
                          (catch Exception e
                            (response/not-found {:message (.getMessage e)})))))}}]

   ["/chapter/:id"
    {:get {:summary "Get details of a single chapter by id."
           :description ""
           :tags ["Chapters"]
           :parameters {:path {:id ::id}}
           :responses {200 {:body ::specs/single-chapter-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [chapter (chapter/get-chapter id)]
                        (response/ok chapter)
                        (response/not-found {:message "Chapter not found"})))}

     :put {:summary "Update the details of a given chapter."
           :description ""
           :no-doc true
           :middleware [mw/check-auth0-jwt]
           :tags ["Chapters" "Updating Existing Records"]
           :parameters {:path {:id ::id}
                        :body ::update-request}
           :responses {200 {:body ::specs/single-chapter-response}
                       400 {:body {:message string?}}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters
                          {:keys [body]} :parameters}]
                      (try
                        (if-let [chapter (chapter/update-chapter! id body)]
                          (response/ok chapter)
                          (response/not-found {:message "Chapter not found"}))
                        (catch Exception e
                          (response/bad-request {:message (.getMessage e)}))))}}]

   ["/create/chapter"
    {:post {:summary "Create a new chapter."
            :description ""
            :no-doc true
            :middleware [mw/check-auth0-jwt]
            :tags ["Chapters" "Adding New Records"]
            :parameters {:body ::create-request}
            :responses {200 {:body {:message string? :id integer?}}
                        400 {:body {:message string? :details any? :parameters any?}}}
            :handler (fn [{:keys [parameters]}]
                       (let [body (:body parameters)]
                         (try
                           (let [id (chapter/create-chapter! body)]
                             (response/ok {:message "Chapter creation successful." :id id}))
                           (catch Exception e
                             (response/bad-request {:message (str "Chapter creation failed: " (.getMessage e))
                                                    :parameters parameters
                                                    :details e})))))}}]])
