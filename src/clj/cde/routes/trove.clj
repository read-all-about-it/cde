(ns cde.routes.trove
  "Trove API proxy routes for fetching external data.

  Provides passthrough endpoints to the National Library of Australia's
  Trove API, translating responses into TBC platform format.

  Features:
  - Fetch newspaper metadata by Trove ID
  - Fetch article/chapter content by Trove ID
  - Check if records already exist in TBC database
  - Sync chapter content from Trove

  See also: [[cde.trove]] for API integration logic."
  (:require
   [ring.util.http-response :as response]
   [cde.trove :as trove]
   [cde.db.chapter :as chapter]
   [cde.db.newspaper :as newspaper]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;;;; Specs

(s/def ::trove_newspaper_id
  (st/spec {:spec int?
            :name "Trove Newspaper ID"
            :description "The Trove-specific ID for a newspaper."
            :json-schema/example 35}))

(s/def ::trove_article_id
  (st/spec {:spec int?
            :name "Trove Article ID"
            :description "The Trove-specific ID for an article/chapter."
            :json-schema/example 123456}))

(defn trove-routes
  "Returns Trove API proxy route definitions.

  Routes:
  - `GET /trove/newspaper/:id` - Fetch newspaper from Trove
  - `GET /trove/chapter/:id` - Fetch article from Trove
  - `PUT /trove/chapter/:id` - Sync chapter from Trove
  - `GET /trove/exists/chapter/:id` - Check if chapter exists
  - `GET /trove/exists/newspaper/:id` - Check if newspaper exists

  Returns: Vector of Reitit route definitions."
  []
  [["/trove/newspaper/:trove_newspaper_id"
    {:get {:summary "Get details of a given newspaper from the Trove API."
           :description "Effectively a 'passthrough', this endpoint will attempt to get the details of a given newspaper from the Trove API, as identified by the 'trove id' (*not* our database id). Translates the response into a format that matches our own platform semantics, and returns it."
           :no-doc true
           :tags ["Trove"]
           :parameters {:path {:trove_newspaper_id ::trove_newspaper_id}}
           :responses {200 {:body ::specs/trove-newspaper-response}
                       400 {:body {:message string? :details any?}}
                       404 {:body {:message string? :details any?}}}
           :handler (fn [{{{:keys [trove_newspaper_id]} :path} :parameters}]
                      (try
                        (let [newspaper (trove/get-newspaper trove_newspaper_id)]
                          (cond (nil? newspaper)
                                (response/not-found {:message "Newspaper not found."
                                                     :details nil})

                                (not= 200 (:trove_api_status newspaper))
                                (response/not-found {:message (str "Newspaper not found. Trove API returned: "
                                                                   (:trove_api_status newspaper))
                                                     :details newspaper})

                                (not (contains? newspaper :title))
                                (response/not-found {:message "Newspaper not found. Trove API returned no title."
                                                     :details newspaper})

                                :else (response/ok (dissoc newspaper :trove_api_status))))
                        (catch Exception e
                          (response/bad-request {:message (str "Error getting newspaper from Trove API: "
                                                               (.getMessage e))
                                                 :details e}))))}}]

   ["/trove/chapter/:trove_article_id"
    {:get {:summary "Get details of a given chapter (ie, article) from the Trove API."
           :description "Effectively a 'passthrough', this endpoint will attempt to get the details of a given chapter from the Trove API, which is an 'article' in a newspaper in their parlance, as identified by trove's 'article id' (not our database's chapter id). Translates the response into a format that matches our own platform semantics, and returns it."
           :no-doc true
           :tags ["Trove"]
           :parameters {:path {:trove_article_id ::trove_article_id}}
           :responses {200 {:body ::specs/trove-article-response}
                       400 {:body {:message string? :details any?}}
                       404 {:body {:message string? :details any?}}}
           :handler (fn [{{{:keys [trove_article_id]} :path} :parameters}]
                      (try
                        (let [article (trove/get-article trove_article_id)]
                          (cond (nil? article)
                                (response/not-found {:message "Article not found."
                                                     :details nil})

                                (not= 200 (:trove_api_status article))
                                (response/not-found {:message (str "Article not found. Trove API returned: "
                                                                   (:trove_api_status article))
                                                     :details article})

                                :else (response/ok (dissoc article :trove_api_status))))
                        (catch Exception e
                          (response/bad-request {:message (str "Error getting article from Trove API: "
                                                               (.getMessage e))
                                                 :details e}))))}

     :put {:summary "Update an existing chapter in our database using details from the Trove API."
           :description "This endpoint will attempt to update an existing chapter in our database using details from the Trove API, which is an 'article' in a newspaper in their parlance, as identified by trove's 'article id' (not our database's chapter id). Translates the response into a format that matches our own platform semantics, updates the relevant chapter (if found), and returns the updated chapter record."
           :no-doc true
           :tags ["Trove" "Chapters" "Updating Existing Records"]
           :parameters {:path {:trove_article_id ::trove_article_id}}
           :responses {200 {:body ::specs/single-chapter-response}
                       400 {:body {:message string? :details any?}}
                       404 {:body {:message string? :details any?}}}
           :handler (fn [{{{:keys [trove_article_id]} :path} :parameters}]
                      (try
                        (let [update (chapter/update-chapter-from-trove! trove_article_id)]
                          (cond (nil? update)
                                (response/not-found {:message "Article not found."
                                                     :details nil})
                                :else (response/ok update)))
                        (catch Exception e
                          (response/bad-request {:message (str "Error updating chapter with details from Trove API: "
                                                               (.getMessage e))
                                                 :details e}))))}}]

   ["/trove/exists/chapter/:trove_article_id"
    {:get {:summary "Check whether a chapter already exists in the TBC database for a given Trove Article ID."
           :description "Takes a Trove Article ID and checks whether our database already contains a chapter record for that article. Returns a boolean value indicating whether the chapter exists or not."
           :no-doc true
           :tags ["Trove" "Chapters"]
           :parameters {:path {:trove_article_id ::trove_article_id}}
           :responses {200 {:body {:exists boolean?
                                   :trove_article_id ::trove_article_id
                                   :chapter_id (s/nilable integer?)}}
                       400 {:body {:message string? :details any?}}}
           :handler (fn [{{{:keys [trove_article_id]} :path} :parameters}]
                      (try
                        (let [chapter-id (chapter/trove-article-id->chapter-id trove_article_id)]
                          (response/ok {:exists (not (nil? chapter-id))
                                        :trove_article_id trove_article_id
                                        :chapter_id chapter-id}))
                        (catch Exception e
                          (response/bad-request {:message (str "Error checking whether chapter exists: "
                                                               (.getMessage e))
                                                 :details e}))))}}]

   ["/trove/exists/newspaper/:trove_newspaper_id"
    {:get {:summary "Check whether a newspaper already exists in the TBC database for a given Trove Newspaper ID."
           :description "Takes a Trove Newspaper ID and checks whether our database already contains a newspaper record for that newspaper. Returns a boolean value indicating whether the newspaper exists or not."
           :no-doc true
           :tags ["Trove" "Newspapers"]
           :parameters {:path {:trove_newspaper_id ::trove_newspaper_id}}
           :responses {200 {:body {:exists boolean?
                                   :trove_newspaper_id ::trove_newspaper_id
                                   :newspaper_table_id ::specs/id}}
                       400 {:body {:message string? :details any?}}}
           :handler (fn [{{{:keys [trove_newspaper_id]} :path} :parameters}]
                      (try
                        (let [newspaper-table-id (newspaper/trove-newspaper-id->newspaper-id trove_newspaper_id)]
                          (response/ok {:exists (not (nil? newspaper-table-id))
                                        :trove_newspaper_id trove_newspaper_id
                                        :newspaper_table_id newspaper-table-id}))
                        (catch Exception e
                          (response/bad-request {:message (str "Error checking whether newspaper exists: "
                                                               (.getMessage e))
                                                 :details e}))))}}]])
