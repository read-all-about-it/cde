(ns cde.routes.platform
  "Platform-level routes for system-wide information and options"
  (:require
   [ring.util.http-response :as response]
   [cde.db.platform :as platform]
   [cde.db.author :as author]
   [cde.db.newspaper :as newspaper]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]))

(defn platform-routes []
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
(ns cde.routes.platform)
