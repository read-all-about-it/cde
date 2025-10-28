(ns cde.routes.platform
  "Platform-level API routes for system-wide information and options.

  Provides endpoints for:
  - Platform statistics (counts of all entities)
  - Search filter options (nationalities, genders)
  - Dropdown data for record creation forms

  These endpoints support the SPA's home page and creation modals."
  (:require
   [ring.util.http-response :as response]
   [cde.db.platform :as platform]
   [cde.db.author :as author]
   [cde.db.newspaper :as newspaper]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]))

(defn platform-routes
  "Returns platform-level route definitions.

  Routes:
  - `GET /platform/statistics` - Entity counts
  - `GET /platform/search-options` - Search filter values
  - `GET /options/newspapers` - Newspaper dropdown list
  - `GET /options/authors` - Author dropdown list

  Returns: Vector of Reitit route definitions."
  []
  [["/platform/statistics"
    {:get {:summary "Get platform statistics: number of titles, authors, chapters, etc."
           :description ""
           :no-doc true
           :tags ["Platform"]
           :responses {200 {:body ::specs/platform-stats-response}
                       400 {:body {:message string?}}}
           :handler (fn [_]
                      (try
                        (let [stats (platform/get-platform-statistics)]
                          (response/ok stats))
                        (catch Exception e
                          (response/not-found {:message (.getMessage e)}))))}}]

   ["/platform/search-options"
    {:get {:summary "Get options used for search: author nationalities, author genders, etc."
           :description ""
           :no-doc true
           :tags ["Platform"]
           :responses {200 {:body {:author-nationalities ::specs/author-nationalities-response
                                   :author-genders ::specs/author-genders-response}}
                       400 {:body {:message string?}}}
           :handler (fn [_]
                      (try
                        (let [nationalities (author/get-nationalities)
                              genders (author/get-genders)]
                          (response/ok {:author-nationalities nationalities
                                        :author-genders genders}))
                        (catch Exception e
                          (response/not-found {:message (.getMessage e)}))))}}]

   ["/options/newspapers"
    {:get {:summary "Get a 'terse' list of all newspapers in the database (for use in creation of new titles)."
           :description ""
           :no-doc true
           :tags ["Platform"]
           :responses {200 {:body  (s/coll-of map?)}
                       400 {:body {:message string?}}}
           :handler (fn [_]
                      (try
                        (let [newspapers (newspaper/get-terse-newspaper-list)]
                          (response/ok newspapers))
                        (catch Exception e
                          (response/not-found {:message (.getMessage e)}))))}}]

   ["/options/authors"
    {:get {:summary "Get a 'terse' list of all authors in the database (for use in creation of new titles)."
           :description ""
           :no-doc true
           :tags ["Platform"]
           :responses {200 {:body (s/coll-of map?)}
                       400 {:body {:message string?}}}
           :handler (fn [_]
                      (try
                        (let [authors (author/get-terse-author-list)]
                          (response/ok authors))
                        (catch Exception e
                          (response/not-found {:message (.getMessage e)}))))}}]])
