(ns cde.routes.search
  "Search functionality routes"
  (:require
   [ring.util.http-response :as response]
   [cde.db.search :as search]
   [cde.db.newspaper :as newspaper]
   [cde.routes.specs :as specs]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

;; Search specs
(s/def ::chapter_text
  (st/spec {:spec (s/and string? #(<= 1 (count %)))
            :name "Chapter Text"
            :description "A (case-insensitive) substring to search for within all chapters in the database."
            :json-schema/example "kangaroo"}))

(s/def ::newspaper_title_text
  (st/spec {:spec string?
            :name "Newspaper Title Text"
            :description "A (case-insensitive) substring to search for within all newspaper titles in the database."
            :json-schema/example "Gazette"}))

(s/def ::author_name
  (st/spec {:spec string?
            :name "Author Name"
            :description "A (case-insensitive) substring to search for within all author names in the database — their common name, as well as any known aliases and publication pseudonyms."
            :json-schema/example "Smith"}))

(s/def ::title_text
  (st/spec {:spec string?
            :name "Title Text"
            :description "A (case-insensitive) substring to search for within all titles in the database."
            :json-schema/example "A Mystery"}))

(s/def ::author_nationality (s/nilable string?))
(s/def ::author_gender (s/nilable string?))
(s/def ::limit (s/nilable int?))
(s/def ::offset (s/nilable int?))
(s/def ::next (s/nilable string?))
(s/def ::previous (s/nilable string?))
(s/def ::trove-newspaper-id int?)

(s/def ::titles-parameters
  (s/keys :opt-un [::title_text
                   ::newspaper_title_text
                   ::author_nationality
                   ::author_name
                   ::author_gender
                   ::limit
                   ::offset]))

(s/def ::chapters-parameters
  (s/keys :opt-un [::chapter_text
                   ::newspaper_title_text
                   ::author_nationality
                   ::author_name
                   ::author_gender
                   ::limit
                   ::offset]))

(s/def ::newspapers-parameters
  (s/keys :opt-un [::trove-newspaper-id]))

(defn search-routes []
  [["/search/titles"
    {:get {:summary "Search for titles."
           :description ""
           :no-doc true
           :tags ["Search"]
           :parameters {:query ::titles-parameters}
           :responses {200 {:body ::specs/search-titles-response}
                       400 {:body {:message string?}}}
           :handler (fn [request-map]
                      (let [query-params (get-in request-map [:parameters :query])]
                        (try
                          (let [search-results (search/search-titles query-params)]
                            (response/ok search-results))
                          (catch Exception e
                            (response/not-found {:message (.getMessage e)})))))}}]

   ["/search/chapters"
    {:get {:summary "Search for chapters."
           :description ""
           :no-doc true
           :tags ["Search"]
           :parameters {:query ::chapters-parameters}
           :responses {200 {:body ::specs/search-chapters-response}
                       400 {:body {:message string?}}}
           :handler (fn [request-map]
                      (let [query-params (get-in request-map [:parameters :query])]
                        (try
                          (let [results (search/search-chapters query-params)]
                            (response/ok results))
                          (catch Exception e
                            (response/not-found {:message (.getMessage e)})))))}}]

   ["/search/newspapers"
    {:get {:summary "Search for newspapers."
           :description "Note: currently only supports a 'trove_newspaper_id' query parameter."
           :no-doc true
           :tags ["Search"]
           :parameters {:query ::newspapers-parameters}
           :responses {200 {:body ::specs/search-newspapers-response}
                       400 {:body {:message string?}}
                       404 {:body {:message string?}}}
           :handler (fn [request-map]
                      (let [query-params (get-in request-map [:parameters :query])]
                        (if-not (contains? query-params :trove-newspaper-id)
                          (response/bad-request {:message "Missing required query parameter 'trove_newspaper_id'"})
                          (try
                            (let [newspapers (newspaper/get-newspaper-by-trove-id (:trove-newspaper-id query-params))]
                              (if (empty? newspapers)
                                (response/not-found {:message "No newspapers found for trove_newspaper_id"})
                                (response/ok newspapers)))
                            (catch Exception e
                              (response/not-found {:message (.getMessage e)}))))))}}]])
(ns cde.routes.search)
