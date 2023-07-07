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
   [cde.db.user :as user]
   [cde.db.search :as search]
   [cde.db.newspaper :as newspaper]
   [cde.db.author :as author]
   [cde.db.title :as title]
   [cde.db.chapter :as chapter]
   [cde.db.platform :as platform]
   [cde.trove :as trove]
   [ring.util.http-response :as response]
   [spec-tools.core :as st]
   [clojure.spec.alpha :as s]
   [reitit.core :as r]
   [cde.middleware :as mw]
   ))

(def ^:private emailregex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email (s/and string? #(re-matches emailregex %)))

(s/def ::user/request-parameters
  (s/keys :req-un [::email]))


(s/def ::newspaper/newspaper_title (s/nilable string?))
(s/def ::newspaper/location (s/nilable string?))
(s/def ::newspaper/details (s/nilable string?))
(s/def ::newspaper/newspaper_type (s/nilable string?))
(s/def ::newspaper/colony_state (s/nilable string?))


(s/def ::author/common_name string?)


(s/def ::author/other_name (s/nilable string?))
(s/def ::author/gender (s/nilable string?))
(s/def ::author/nationality (s/nilable string?))
(s/def ::author/nationality_details (s/nilable string?))


(s/def ::author/author_details (s/nilable string?))

(s/def ::title/publication_title (s/nilable string?))
(s/def ::title/attributed_author_name (s/nilable string?))
(s/def ::title/author_of (s/nilable string?))
(s/def ::title/additional_info (s/nilable string?))
(s/def ::title/inscribed_author_nationality (s/nilable string?))
(s/def ::title/inscribed_author_gender (s/nilable string?))
(s/def ::title/information_source (s/nilable string?))
(s/def ::title/length (s/nilable int?))
(s/def ::title/trove_source (s/nilable string?))
(s/def ::title/also_published (s/nilable string?))
(s/def ::title/name_category (s/nilable string?))
(s/def ::title/curated_dataset (s/nilable boolean?))
(s/def ::chapter/chapter_number (s/nilable string?))
(s/def ::chapter/chapter_title (s/nilable string?))
(s/def ::chapter/article_url (s/nilable string?))
(s/def ::chapter/page_references (s/nilable int?))
(s/def ::chapter/page_url (s/nilable string?))
(s/def ::chapter/word_count (s/nilable int?))
(s/def ::chapter/illustrated (s/nilable boolean?))
(s/def ::chapter/page_sequence (s/nilable string?))
(s/def ::chapter/chapter_html (s/nilable string?))
(s/def ::chapter/chapter_text (s/nilable string?))
(s/def ::chapter/text_title (s/nilable string?))
(s/def ::chapter/export_title (s/nilable string?))


;; SPECS FOR 'ID' FIELDS
(s/def ::pk-id (s/and int? pos?)) ;; a positive integer 'primary key' id

(s/def ::chapter/id
  (st/spec {:spec ::pk-id
            :name "Chapter ID"
            :description "The unique ID of the chapter."
            :json-schema/example 1}))

(s/def ::newspaper/id
  (st/spec {:spec ::pk-id
            :name "Newspaper ID"
            :description "The unique ID of the newspaper."
            :json-schema/example 1}))


(s/def ::author/id
  (st/spec {:spec ::pk-id
            :name "Author ID"
            :description "The unique ID of the author."
            :json-schema/example 1}))


(s/def ::user/id
  (st/spec {:spec ::pk-id
            :name "User ID"
            :description "The unique ID of the user."
            :json-schema/example 1}))

(s/def ::title/id
  (st/spec {:spec ::pk-id
            :name "Title ID"
            :description "The unique ID of the title."
            :json-schema/example 1}))




(s/def ::user/email
  (st/spec {:spec ::email
            :name "User Email"
            :description "The email address of the user."
            :json-schema/example "test@example.com"}))





;; SPECS FOR 'FOREIGN KEY' FIELDS
(s/def ::title/added_by (s/nilable ::user/id))
(s/def ::chapter/added_by (s/nilable ::user/id))
(s/def ::newspaper/added_by (s/nilable ::user/id))
(s/def ::author/added_by (s/nilable ::user/id))

(s/def ::title/author_id (s/nilable ::author/id))

(s/def ::chapter/title_id ::title/id)

(s/def ::title/newspaper_table_id ::newspaper/id) ;; the FK to the newspaper table



(s/def ::trove/trove_newspaper_id
  (st/spec {:spec (s/and int? pos?)
            :name "Trove Newspaper ID"
            :description "The unique ID of the newspaper as it's recorded in the NLA's Trove database.

eg \"https://api.trove.nla.gov.au/v3/newspaper/title/35?key=YOURKEY\"
where '35' is the Trove Newspaper ID.

The NLA refers to this as the 'NLA Object ID' in some instances.

For more details, see: https://trove.nla.gov.au/about/create-something/using-api/v3/api-technical-guide#get-information-about-one-newspaper-or-gazette-title"
            :json-schema/example 35}))

(s/def ::trove/trove_article_id
  (st/spec {:spec (s/and int? pos?)
            :name "Trove Article ID"
            :description "The unique ID of an article that appears in the NLA's Trove database.

eg \"https://api.trove.nla.gov.au/v3/newspaper/1390875?key=YOURKEY\"
where '1390875' is the Trove Article ID.

For more details, see: https://trove.nla.gov.au/about/create-something/using-api/v3/api-technical-guide#api-newspaper-and-gazette-article-record-structure"
            :json-schema/example 1390875}))

(s/def ::chapter/trove_article_id ::trove/trove_article_id)

;; SPECS FOR DATES OF PUBLICATION, 'SPAN' DATES, ETC
(s/def ::date-string (s/and string?
                            #(re-matches #"^\d{4}-\d{2}-\d{2}$" %)))
(s/def ::chapter/dow
  (st/spec {:spec (s/and string?
                         #(contains? #{"Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday"} %))
            :name "Day Of Week"
            :description "The day of the week on which the chapter was published."
            :json-schema/example "Monday"}))
(s/def ::chapter/pub_day
  (st/spec {:spec (s/and int? pos? #(<= 1 % 31))
            :name "Day Of Month"
            :description "The day of the month on which the chapter was published."
            :json-schema/example 29}))
(s/def ::chapter/pub_month
  (st/spec {:spec (s/and int? pos? #(<= 1 % 12))
            :name "Month Of Year"
            :description "The month of the year in which the chapter was published."
            :json-schema/example 1}))
(s/def ::chapter/pub_year
  (st/spec {:spec (s/and int? pos?)
            :name "Year Of Publication"
            :description "The year in which the chapter was published."
            :json-schema/example 1901}))
(s/def ::newspaper/start_year
  (st/spec {:spec (s/and int? pos?)
            :name "Start Year"
            :description "The year in which the newspaper began publication."
            :json-schema/example 1901}))
(s/def ::newspaper/end_year
  (st/spec {:spec (s/and int? pos?)
            :name "End Year"
            :description "The year in which the newspaper ceased publication."
            :json-schema/example 1901}))
(s/def ::newspaper/start_date
  (st/spec {:spec ::date-string
            :name "Start Date"
            :description "The date on which the newspaper began publication."
            :json-schema/example "1901-01-01"}))
(s/def ::newspaper/end_date
  (st/spec {:spec ::date-string
            :name "End Date"
            :description "The date on which the newspaper ceased publication."
            :json-schema/example "1901-01-01"}))
(s/def ::title/span_start
  (st/spec {:spec ::date-string
            :name "Span Start"
            :description "The date on which the title's publication span began. This is the earliest date on which any chapter in the title was published."
            :json-schema/example "1901-01-01"}))
(s/def ::title/span_end
  (st/spec {:spec ::date-string
            :name "Span End"
            :description "The date on which the title's publication span ended. This is the latest date on which any chapter in the title was published."
            :json-schema/example "1901-01-01"}))
(s/def ::chapter/final_date
  (st/spec {:spec ::date-string
            :name "Final Date"
            :description "The date on which the chapter was published."
            :json-schema/example "1901-01-01"}))
(s/def ::title/common_title
  (st/spec {:spec (s/nilable string?)
            :name "Common Title"
            :description "The common title of the title. This is the title that is most commonly known as, but not necessarily the title it was published as in a given newspaper."}))


;; SPECS FOR NEWSPAPER DETAILS
(s/def ::newspaper/title
  (st/spec {:spec string?
            :name "Title"
            :description "The full title of the newspaper."
            :json-schema/example "The Sydney Morning Herald (NSW : 1842 - 1954)"}))

(s/def ::newspaper/common_title
  (st/spec {:spec (s/nilable string?)
            :name "Common Title"
            :description "The common title of the newspaper. This is the title that is most commonly used to refer to the newspaper."
            :json-schema/example "The Sydney Morning Herald"}))

(s/def ::newspaper/issn
  (st/spec {:spec (s/and string?
                         #(re-matches #"^[0-9]{4}-?[0-9]{3}[0-9xX]$" %))
            :name "ISSN"
            :description "The International Standard Serial Number (ISSN) of the newspaper. An 8-digit code, usually separated by a hyphen into two 4-digit numbers."
            :json-schema/example "0312-6315"}))

(s/def ::newspaper/trove_newspaper_id ::trove/trove_newspaper_id)






(s/def ::create-newspaper-request
  (s/keys :req-un [::trove/trove_newspaper_id
                   ::newspaper/title]
          :opt-un [::newspaper/common_title
                   ::newspaper/location
                   ::newspaper/start_year
                   ::newspaper/end_year
                   ::newspaper/details
                   ::newspaper/newspaper_type
                   ::newspaper/colony_state
                   ::newspaper/start_date
                   ::newspaper/end_date
                   ::newspaper/issn
                   ::newspaper/added_by]))

(s/def ::create-author-request
  (s/keys :req-un [::author/common_name]
          :opt-un [::author/other_name
                   ::author/gender
                   ::author/nationality
                   ::author/nationality_details
                   ::author/author_details
                   ::author/added_by]))

(s/def ::create-title-request
  (s/keys :req-un [::title/newspaper_table_id
                   ::title/author_id]
          :opt-un [::title/span_start
                   ::title/span_end
                   ::title/publication_title
                   ::title/attributed_author_name
                   ::title/common_title
                   ::title/author_of
                   ::title/additional_info
                   ::title/inscribed_author_nationality
                   ::title/inscribed_author_gender
                   ::title/information_source
                   ::title/length
                   ::title/trove_source
                   ::title/also_published
                   ::title/name_category
                   ::title/curated_dataset
                   ::title/added_by]))

(s/def ::update-title-request map?)

(s/def ::update-author-request map?)




(s/def ::create-chapter-request
  (s/keys :req-un [::chapter/title_id
                   ::chapter/trove_article_id]
          :opt-un [::chapter/chapter_number
                   ::chapter/chapter_title
                   ::chapter/article_url
                   ::chapter/dow
                   ::chapter/pub_day
                   ::chapter/pub_month
                   ::chapter/pub_year
                   ::chapter/final_date
                   ::chapter/page_references
                   ::chapter/page_url
                   ::chapter/word_count
                   ::chapter/illustrated
                   ::chapter/page_sequence
                   ::chapter/chapter_html
                   ::chapter/chapter_text
                   ::chapter/text_title
                   ::chapter/export_title
                   ::chapter/added_by]))

(s/def ::profile-response map?)
(s/def ::newspaper-response map?)
(s/def ::author-response map?)
(s/def ::single-title-response map?)
(s/def ::single-chapter-response map?)
(s/def ::platform-stats-response map?)

(s/def ::author-nationalities-response (s/nilable (s/coll-of string?)))
(s/def ::author-genders-response (s/nilable (s/coll-of string?)))

(s/def ::author/gender (s/nilable string?))


(s/def ::search/limit
  (st/spec {:spec (s/and int? #(<= 1 % 100))
            :name "Limit"
            :description "The maximum number of results to return"
            :json-schema/default 50}))
(s/def ::search/offset
  (st/spec {:spec (s/and int? #(>= % 0))
            :name "Offset"
            :description "The number of results to skip"
            :json-schema/default 0}))
(s/def ::search/search_type
  (st/spec {:spec (s/and string? #{"title" "chapter"})
            :name "Search Type"
            :description "The type of search performed. Must be either 'title' or 'chapter'."
            :json-schema/example "title"}))

(s/def ::title/results
  (st/spec {:spec (s/coll-of ::single-title-response)
            :name "Results"
            :description "The results of the search; a list of titles."}))

(s/def ::chapter/results
  (st/spec {:spec (s/coll-of ::single-chapter-response)
            :name "Results"
            :description "The results of the search; a list of chapters."}))

(s/def ::search/chapter_text
  (st/spec {:spec (s/and string? #(<= 1 (count %)))
            :name "Chapter Text"
            :description "A (case-insensitive) substring to search for within all chapters in the database."
            :json-schema/example "kangaroo"}))

(s/def ::search/newspaper_title_text
  (st/spec {:spec string?
            :name "Newspaper Title Text"
            :description "A (case-insensitive) substring to search for within all newspaper titles in the database."
            :json-schema/example "Gazette"}))


(s/def ::search/author_name
  (st/spec {:spec string?
            :name "Author Name"
            :description "A (case-insensitive) substring to search for within all author names in the database — their common name, as well as any known aliases and publication pseudonyms."
            :json-schema/example "Smith"}))

(s/def ::search/title_text
  (st/spec {:spec string?
            :name "Title Text"
            :description "A (case-insensitive) substring to search for within all titles in the database."
            :json-schema/example "A Mystery"}))

(s/def ::search/author_nationality (s/nilable string?))

(s/def ::search/author_gender (s/nilable string?))

(s/def ::search/titles-parameters
  (s/keys :opt-un [::search/title_text
                   ::search/newspaper_title_text
                   ::search/author_nationality
                   ::search/author_name
                   ::search/author_gender
                   ::search/limit
                   ::search/offset]))
(s/def ::search/titles-response
  (s/keys :req-un [::search/limit
                   ::search/offset
                   ::title/results
                   ::search/search_type]))

(s/def ::search/chapters-parameters
  (s/keys :req-un [::search/chapter_text]
          :opt-un [::search/newspaper_title_text
                   ::search/title_text
                   ::search/author_nationality
                   ::search/author_name
                   ::search/author_gender]))
(s/def ::search/chapters-response
  (s/keys :req-un [::search/limit
                   ::search/offset
                   ::chapter/results
                   ::search/search_type]))


(s/def ::search/newspapers-parameters
  (s/keys :req-un [::newspaper/trove_newspaper_id]))
(s/def ::search/newspapers-response
  (s/coll-of ::newspaper-response))



(s/def ::chapters-within-title-response (s/nilable (s/coll-of ::single-chapter-response)))
(s/def ::titles-by-author-response (s/nilable (s/coll-of ::single-title-response)))


(s/def ::trove-newspaper-response map?)
(s/def ::trove-article-response map?)










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
                 multipart/multipart-middleware
                 ;; test custom middleware
                 mw/test-middleware
                 ;; auth0 middleware
                ;;  mw/wrap-auth0
                 ]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "To Be Continued API"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]
   ["/v1"

    ["/test"
     {:middleware [mw/print-auth0-header]
      :get {:summary "A test endpoint."
            :description ""
            :tags ["Test"]
            :responses {200 {:body {:message string?
                                    :now string?}}
                        400 {:body {:message string?}}}
            :handler
            (fn [request-map]
              ;; (println "User:" (:user request-map))
              (response/ok {:message "Hello, world!"
                            :now (str (java.util.Date.))}))}}]

    ["/user"
     {:get {:summary "Get a user/email map given an email (passed in query params), creating a user record if necessary."
            :description ""
            :tags ["User"]
            :parameters {:query ::user/request-parameters}
            :responses {200 {:body {:id ::user/id
                                    :email ::user/email}}
                        400 {:body {:message string?}}}
            :handler
            ;; to test the endpoint for now, let's just echo back the email from the query params
            (fn [request-map]
              (let [query (get-in request-map [:parameters :query])
                    email (:email query)]
                ;; (println query)
                (try
                  (let [user (user/get-or-create-user! email)]
                    (response/ok {:id (:id user) :email (:email user)}))
                  (catch Exception e
                    (response/bad-request {:message (.getMessage e)})))))}}]

    ["/platform/statistics"
     {:get {:summary "Get platform statistics: number of titles, authors, chapters, etc."
            :description ""
            :tags ["Platform"]
            :responses {200 {:body ::platform-stats-response}
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
            :tags ["Platform"]
            :responses {200 {:body {:author-nationalities ::author-nationalities-response
                                    :author-genders ::author-genders-response}}
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
            :tags ["Platform"]
            :responses {200 {:body (s/coll-of map?)}
                        400 {:body {:message string?}}}
            :handler (fn [_]
                       (try
                         (let [authors (author/get-terse-author-list)]
                           (response/ok authors))
                         (catch Exception e
                           (response/not-found {:message (.getMessage e)}))))}}]



    ["/search/titles"
     {:get {:summary "Search for titles."
            :description ""
            :tags ["Search"]
            :parameters {:query ::search/titles-parameters}
            :responses {200 {:body ::search/titles-response}
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
            :tags ["Search"]
            :parameters {:query ::search/chapters-parameters}
            :responses {200 {:body ::search/chapters-response}
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
            :tags ["Search"]
            :parameters {:query ::search/newspapers-parameters}
            :responses {200 {:body ::search/newspapers-response}
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
                               (response/not-found {:message (.getMessage e)}))))))}}]

  ;;  ["/user/:id/collections"
  ;;   {:get {:summary "Get a list of all collections of a single user."
  ;;          :description ""
  ;;          :parameters {:path {:id ::user/id}}
  ;;          :responses {200 {:body ::collections-response}
  ;;                      404 {:body {:message string?}}}
  ;;          :handler (fn [{{{:keys [id]} :path} :parameters}]
  ;;                     (if-let [collections (collection/get-user-collections id)]
  ;;                       (response/ok collections)
  ;;                       (response/not-found {:message "User collections not found"})))}}]

  ;;  ["/user/:id/bookmarks"
  ;;   {:get {:summary "Get a list of all items bookmarked by a user, regardless of the bookmark collection the user has put them in."
  ;;          :description ""
  ;;          :parameters {:path {:id ::user/id}}
  ;;          :responses {200 {:body ::bookmarks-response}
  ;;                      404 {:body {:message string?}}}
  ;;          :handler (fn [{{{:keys [id]} :path} :parameters}]
  ;;                     (if-let [bookmarks (collection/get-all-user-collection-items id)]
  ;;                       (response/ok bookmarks)
  ;;                       (response/not-found {:message "User bookmarks not found"})))}}]

    ["/newspaper/:id"
     {:get {:summary "Get details of a single newspaper."
            :description ""
            :tags ["Newspapers"]
            :parameters {:path {:id ::newspaper/id}}
            :responses {200 {:body ::newspaper-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [newspaper (newspaper/get-newspaper id)]
                         (response/ok newspaper)
                         (response/not-found {:message "Newspaper not found"})))}}]

    ["/newspaper/:id/titles"
     {:get {:summary "Get a list of all titles published in a given newspaper."
            :description ""
            :tags ["Newspapers"]
            :parameters {:path {:id ::newspaper/id}}
            :responses {200 {:body ::chapters-within-title-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [titles (newspaper/get-titles-in-newspaper id)]
                         (response/ok titles)
                         (response/not-found {:message "No titles found"})))}}]

    ["/author/:id"
     {:get {:summary "Get details of a single author by id."
            :description ""
            :tags ["Authors"]
            :parameters {:path {:id ::author/id}}
            :responses {200 {:body ::author-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [author (author/get-author id)]
                         (response/ok author)
                         (response/not-found {:message "Author not found"})))}
      :put {:summary "Update the details of a given author."
            :description ""
            :tags ["Authors" "Updating Existing Records"]
            :parameters {:path {:id ::author/id}
                         :body ::update-author-request}
            :responses {200 {:body ::author-response}
                        400 {:body {:message string?}}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters
                           {:keys [body]} :parameters}]
                       (try (if-let [author (author/update-author! id body)]
                              (response/ok author)
                              (response/not-found {:message "Author not found"}))
                            (catch Exception e
                              (response/bad-request {:message (.getMessage e)}))))}}]

    ["/author/:id/titles"
     {:get {:summary "Get a list of all titles by a single author (matched to that author's id)."
            :description ""
            :tags ["Authors"]
            :parameters {:path {:id ::author/id}}
            :responses {200 {:body ::titles-by-author-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [titles (author/get-titles-by-author id)]
                         (response/ok titles)
                         (response/not-found {:message "No titles found for that author."})))}}]

    ["/author-nationalities"
     {:get {:summary "Get a list of all nationalities currently listed in our authors records."
            :description ""
            :tags ["Authors"]
            :responses {200 {:body ::author-nationalities-response}
                        404 {:body {:message string?}}}
            :handler (fn [_]
                       (if-let [nationalities (author/get-nationalities)]
                         (response/ok nationalities)
                         (response/not-found {:message "No nationalities found"})))}}]

    ["/author-genders"
     {:get {:summary "Get a list of all genders currently listed in our authors records."
            :description ""
            :tags ["Authors"]
            :responses {200 {:body ::author-genders-response}
                        404 {:body {:message string?}}}
            :handler (fn [_]
                       (if-let [genders (author/get-genders)]
                         (response/ok genders)
                         (response/not-found {:message "No genders found"})))}}]

    ["/title/:id"
     {:get {:summary "Get details of a single title by id."
            :description ""
            :tags ["Titles"]
            :parameters {:path {:id ::title/id}}
            :responses {200 {:body ::single-title-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [title (title/get-title id true)]
                         (response/ok title)
                         (response/not-found {:message "Title not found"})))}
      :put {:summary "Update the details of a given title."
            :description ""
            :tags ["Titles" "Updating Existing Records"]
            :parameters {:path {:id ::title/id}
                         :body ::update-title-request} ;; TODO: spec this body properly
            :responses {200 {:body ::single-title-response}
                        400 {:body {:message string?}}
                        404 {:body {:message string?}}}
            :handler
            (fn [{{{:keys [id]} :path} :parameters
                  {:keys [body]} :parameters}]
                       ; print out the id and update-fields to the console
              (println "Updating title " id " with fields: " body)
              (try
                (if-let [title (title/update-title! id body)]
                  (response/ok title)
                  (response/not-found {:message "Title not found"}))
                (catch Exception e
                  (println "Error updating title: " (.getMessage e))
                  (response/bad-request {:message (.getMessage e)}))))}}]

    ["/title/:id/chapters"
     {:get {:summary "Get a list of all chapters in a given title."
            :description ""
            :tags ["Titles"]
            :parameters {:path {:id ::title/id}}
            :responses {200 {:body ::chapters-within-title-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [chapters (chapter/get-chapters-in-title id)]
                         (response/ok chapters)
                         (response/not-found {:message "No chapters found"})))}}]

    ["/chapter/:id"
     {:get {:summary "Get details of a single chapter by id."
            :description ""
            :tags ["Chapters"]
            :parameters {:path {:id ::chapter/id}}
            :responses {200 {:body ::single-chapter-response}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [chapter (chapter/get-chapter id)]
                         (response/ok chapter)
                         (response/not-found {:message "Chapter not found"})))}}]


    ["/create/newspaper"
     {:post {:summary "Create a new newspaper."
             :description ""
             :tags ["Newspapers", "Adding New Records"]
             :parameters {:body ::create-newspaper-request}
             :responses {200 {}
                         400 {:body {:message string?}}}
             :handler (fn [{:keys [parameters]}]
                        (let [body (:body parameters)]
                          (try
                            (let [id (newspaper/create-newspaper! body)]
                              (response/ok {:message "Newspaper creation successful."
                                            :id id}))
                            (catch Exception e
                              (response/bad-request {:message (str "Newspaper creation failed: " (.getMessage e))})))))}}]

    ["/create/author"
     {:post {:summary "Create a new author."
             :description ""
             :tags ["Authors", "Adding New Records"]
             :parameters {:body ::create-author-request}
             :responses {200 {:body {:message string? :id integer?}}
                         400 {:body {:message string?}}}
             :handler (fn [{:keys [parameters]}]
                        (let [body (:body parameters)]
                          (try
                            (let [id (author/create-author! body)]
                              (response/ok {:message "Author creation successful." :id id}))
                            (catch Exception e
                              (response/bad-request {:message (str "Author creation failed: " (.getMessage e))})))))}}]

    ["/create/title"
     {:post {:summary "Create a new title."
             :description ""
             :tags ["Titles", "Adding New Records"]
             :parameters {:body ::create-title-request}
             :responses {200 {:body {:message string?
                                     :id ::title/id}}
                         400 {:body {:message string?}}}
             :handler (fn [{:keys [parameters]}]
                        (let [body (:body parameters)]
                          (try
                            (let [id (title/create-title! body)]
                              (response/ok {:message "Title creation successful." :id id}))
                            (catch Exception e
                              (response/bad-request {:message (str "Title creation failed: " (.getMessage e))})))))}}]

    ["/create/chapter"
     {:post {:summary "Create a new chapter."
             :description ""
             :tags ["Chapters", "Adding New Records"]
             :parameters {:body ::create-chapter-request}
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
                                                     :details e})))))}}]

    ["/trove/newspaper/:trove_newspaper_id"
     {:get {:summary "Get details of a given newspaper from the Trove API."
            :description "Effectively a 'passthrough', this endpoint will attempt to get the details of a given newspaper from the Trove API, as identified by the 'trove id' (*not* our database id). Translates the response into a format that matches our own platform semantics, and returns it."
            :tags ["Trove"]
            :parameters {:path {:trove_newspaper_id ::trove/trove_newspaper_id}}
            :responses {200 {:body ::trove-newspaper-response}
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
            :tags ["Trove"]
            :parameters {:path {:trove_article_id ::trove/trove_article_id}}
            :responses {200 {:body ::trove-article-response}
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
            :tags ["Trove" "Chapters" "Updating Existing Records"]
            :parameters {:path {:trove_article_id ::trove/trove_article_id}}
            :responses {200 {:body ::single-chapter-response}
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
            :description
            "Takes a Trove Article ID and checks whether our database already contains a chapter record for that article. Returns a boolean value indicating whether the chapter exists or not."
            :tags ["Trove" "Chapters"]
            :parameters {:path {:trove_article_id ::trove/trove_article_id}}
            :responses {200 {:body {:exists boolean?
                                    :trove_article_id ::trove/trove_article_id
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
    ["trove/exists/newspaper/:trove_newspaper_id"
     {:get {:summary "Check whether a newspaper already exists in the TBC database for a given Trove Newspaper ID."
            :description "Takes a Trove Newspaper ID and checks whether our database already contains a newspaper record for that newspaper. Returns a boolean value indicating whether the newspaper exists or not."
            :tags ["Trove" "Newspapers"]
            :parameters {:path {:trove_newspaper_id ::trove/trove_newspaper_id}}
            :responses {200 {:body {:exists boolean?
                                    :trove_newspaper_id ::trove/trove_newspaper_id
                                    :newspaper_table_id ::newspaper/id}}
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
                                                  :details e}))))}}]]])