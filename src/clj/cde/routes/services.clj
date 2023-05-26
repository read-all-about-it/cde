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
   [cde.auth :as auth]
   [cde.search :as search]
   [cde.newspaper :as newspaper]
   [cde.author :as author]
   [cde.title :as title]
   [cde.chapter :as chapter]
   [cde.platform :as platform]
   [ring.util.http-response :as response]
   [spec-tools.core :as st]
   [clojure.spec.alpha :as s]
   [reitit.core :as r]))

(def ^:private emailregex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email (s/and string? #(re-matches emailregex %)))


(s/def ::trove-newspaper-id int?)
(s/def ::newspaper-title (s/nilable string?))
(s/def ::common-title (s/nilable string?))
(s/def ::location (s/nilable string?))
(s/def ::details (s/nilable string?))
(s/def ::newspaper-type (s/nilable string?))
(s/def ::colony-state (s/nilable string?))
(s/def ::common-name string?)
(s/def ::other-name (s/nilable string?))
(s/def ::gender (s/nilable string?))
(s/def ::nationality (s/nilable string?))
(s/def ::nationality-details (s/nilable string?))
(s/def ::author-details (s/nilable string?))

(s/def ::publication-title (s/nilable string?))
(s/def ::attributed-author-name (s/nilable string?))
(s/def ::author-of (s/nilable string?))
(s/def ::additional-info (s/nilable string?))
(s/def ::inscribed-author-nationality (s/nilable string?))
(s/def ::inscribed-author-gender (s/nilable string?))
(s/def ::information-source (s/nilable string?))
(s/def ::length (s/nilable int?))
(s/def ::trove-source (s/nilable string?))
(s/def ::also-published (s/nilable string?))
(s/def ::name-category (s/nilable string?))
(s/def ::curated-dataset (s/nilable boolean?))
(s/def ::chapter-number (s/nilable string?))
(s/def ::chapter-title (s/nilable string?))
(s/def ::article-url (s/nilable string?))
(s/def ::page-references (s/nilable int?))
(s/def ::page-url (s/nilable string?))
(s/def ::word-count (s/nilable int?))
(s/def ::illustrated (s/nilable boolean?))
(s/def ::page-sequence (s/nilable string?))
(s/def ::chapter-html (s/nilable string?))
(s/def ::chapter-text (s/nilable string?))
(s/def ::text-title (s/nilable string?))
(s/def ::export-title (s/nilable string?))


;; SPECS FOR 'ID' FIELDS
(s/def ::pk-id (s/and int? pos?)) ;; a positive integer 'primary key' id
(s/def ::id
  (st/spec {:spec ::pk-id
            :name "ID"
            :description "The primary key ID of the record."
            :json-schema/example 1}))
(s/def ::title-id
  (st/spec {:spec ::pk-id
            :name "Title ID"
            :description "The unique ID of the title."
            :json-schema/example 1}))
(s/def ::chapter-id
  (st/spec {:spec ::pk-id
            :name "Chapter ID"
            :description "The unique ID of the chapter."
            :json-schema/example 1}))
(s/def ::newspaper-id
  (st/spec {:spec ::pk-id
            :name "Newspaper ID"
            :description "The unique ID of the newspaper."
            :json-schema/example 1}))
(s/def ::newspaper-table-id
  (st/spec {:spec ::pk-id
            :name "Newspaper Table ID"
            :description "The unique ID of a newspaper in our newspaper table."
            :json-schema/example 1}))
(s/def ::author-id
  (st/spec {:spec ::pk-id
            :name "Author ID"
            :description "The unique ID of the author."
            :json-schema/example 1}))
(s/def ::user-id
  (st/spec {:spec ::pk-id
            :name "User ID"
            :description "The unique ID of the user."
            :json-schema/example 1}))
(s/def ::added-by
  (st/spec {:spec ::pk-id
            :name "User ID"
            :description "The unique ID of the user who first contributed the record to our database."}))

(s/def ::trove-newspaper-id
  (st/spec {:spec (s/and int? pos?)
            :name "Trove Newspaper ID"
            :description "The unique ID of the newspaper as it's recorded in the NLA's Trove database.

                          eg 'https://api.trove.nla.gov.au/v3/newspaper/title/35?key=YOURKEY'
                          where '35' is the Trove Newspaper ID.
                          
                          The NLA refers to this as the 'NLA Object ID' in some instances.
                          
                          For more details, see: https://trove.nla.gov.au/about/create-something/using-api/v3/api-technical-guide#get-information-about-one-newspaper-or-gazette-title"
            :json-schema/example 35}))
(s/def ::trove-article-id
  (st/spec {:spec (s/and int? pos?)
            :name "Trove Article ID"
            :description "The unique ID of an article that appears in the NLA's Trove database."
            :json-schema/example 1}))

;; SPECS FOR DATES OF PUBLICATION, 'SPAN' DATES, ETC
(s/def ::date-string (s/and string?
                            #(re-matches #"^\d{4}-\d{2}-\d{2}$" %)))
(s/def ::dow
  (st/spec {:spec (s/and string?
                         #(contains? #{"Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday"} %))
            :name "Day Of Week"
            :description "The day of the week on which the chapter was published."
            :json-schema/example "Monday"}))
(s/def ::pub-day
  (st/spec {:spec (s/and int? pos? #(<= 1 % 31))
            :name "Day Of Month"
            :description "The day of the month on which the chapter was published."
            :json-schema/example 29}))
(s/def ::pub-month
  (st/spec {:spec (s/and int? pos? #(<= 1 % 12))
            :name "Month Of Year"
            :description "The month of the year in which the chapter was published."
            :json-schema/example 1}))
(s/def ::pub-year
  (st/spec {:spec (s/and int? pos?)
            :name "Year Of Publication"
            :description "The year in which the chapter was published."
            :json-schema/example 1901}))
(s/def ::start-year
  (st/spec {:spec (s/and int? pos?)
            :name "Start Year"
            :description "The year in which the newspaper began publication."
            :json-schema/example 1901}))
(s/def ::end-year
  (st/spec {:spec (s/and int? pos?)
            :name "End Year"
            :description "The year in which the newspaper ceased publication."
            :json-schema/example 1901}))
(s/def ::start-date
  (st/spec {:spec ::date-string
            :name "Start Date"
            :description "The date on which the newspaper began publication."
            :json-schema/example "1901-01-01"}))
(s/def ::end-date
  (st/spec {:spec ::date-string
            :name "End Date"
            :description "The date on which the newspaper ceased publication."
            :json-schema/example "1901-01-01"}))
(s/def ::span-start
  (st/spec {:spec ::date-string
            :name "Span Start"
            :description "The date on which the title's publication span began. This is the earliest date on which any chapter in the title was published."
            :json-schema/example "1901-01-01"}))
(s/def ::span-end
  (st/spec {:spec ::date-string
            :name "Span End"
            :description "The date on which the title's publication span ended. This is the latest date on which any chapter in the title was published."
            :json-schema/example "1901-01-01"}))
(s/def ::final-date
  (st/spec {:spec ::date-string
            :name "Final Date"
            :description "The date on which the chapter was published."
            :json-schema/example "1901-01-01"} ))


;; SPECS FOR NEWSPAPER DETAILS
(s/def ::newspaper/title
  (st/spec {:spec string?
            :name "Title"
            :description "The full title of the newspaper."
            :json-schema/example "The Sydney Morning Herald (NSW : 1842 - 1954)"}))

(s/def ::newspaper/common-title
  (st/spec {:spec string?
            :name "Common Title"
            :description "The common title of the newspaper. This is the title that is most commonly used to refer to the newspaper."
            :json-schema/example "The Sydney Morning Herald"}))

(s/def ::issn
  (st/spec {:spec (s/and string?
                         #(re-matches #"^[0-9]{4}-?[0-9]{3}[0-9xX]$" %))
            :name "ISSN"
            :description "The International Standard Serial Number (ISSN) of the newspaper. An 8-digit code, usually separated by a hyphen into two 4-digit numbers."
            :json-schema/example "0312-6315"}))

(s/def ::create-newspaper-request
  (s/keys :req-un [::trove-newspaper-id
                   ::newspaper/title]
          :opt-un [::newspaper/common-title
                   ::location
                   ::start-year
                   ::end-year
                   ::details
                   ::newspaper-type
                   ::colony-state
                   ::start-date
                   ::end-date
                   ::issn
                   ::added-by]))

(s/def ::create-author-request
  (s/keys :req-un [::common-name]
          :opt-un [::other-name
                   ::gender
                   ::nationality
                   ::nationality-details
                   ::author-details
                   ::added-by]))

(s/def ::create-title-request
  (s/keys :req-un [::newspaper-table-id
                   ::author-id]
          :opt-un [::span-start
                   ::span-end
                   ::publication-title
                   ::attributed-author-name
                   ::common-title
                   ::author-of
                   ::additional-info
                   ::inscribed-author-nationality
                   ::inscribed-author-gender
                   ::information-source
                   ::length
                   ::trove-source
                   ::also-published
                   ::name-category
                   ::curated-dataset
                   ::added-by]))

(s/def ::create-chapter-request
  (s/keys :req-un [::title-id]
          :opt-un [::chapter-number
                   ::chapter-title
                   ::article-url
                   ::dow
                   ::pub-day
                   ::pub-month
                   ::pub-year
                   ::final-date
                   ::page-references
                   ::page-url
                   ::word-count
                   ::illustrated
                   ::page-sequence
                   ::chapter-html
                   ::chapter-text
                   ::text-title
                   ::export-title
                   ::added-by
                   ::trove-article-id]))

(s/def ::profile-response map?)
(s/def ::newspaper-response map?)
(s/def ::author-response map?)
(s/def ::single-title-response map?)
(s/def ::single-chapter-response map?)
(s/def ::platform-stats-response map?)

(s/def ::author-nationalities-response (s/nilable (s/coll-of string?)))
(s/def ::author-genders-response (s/nilable (s/coll-of string?)))

(s/def ::author (s/nilable string?))
(s/def ::gender (s/nilable string?))

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

(s/def ::search/chapter-text
  (st/spec {:spec (s/and string? #(<= 1 (count %)))
            :name "Chapter Text"
            :description "A (case-insensitive) substring to search for within all chapters in the database."
            :json-schema/example "kangaroo"}))

(s/def ::search/titles-parameters
  (s/keys :opt-un [::common-title
                   ::newspaper-title
                   ::nationality
                   ::author
                   ::search/limit
                   ::search/offset]))
(s/def ::search/titles-response
  (s/keys :req-un [::search/limit
                   ::search/offset
                   ::title/results
                   ::search/search_type]))

(s/def ::search/chapters-parameters
  (s/keys :req-un [::search/chapter-text]
          :opt-un [::newspaper-title
                   ::common-title
                   ::nationality
                   ::author]))

(s/def ::search/chapters-response
  (s/keys :req-un [::search/limit
                   ::search/offset
                   ::chapter/results
                   ::search/search_type]))

(s/def ::chapters-within-title-response (s/nilable (s/coll-of ::single-chapter-response)))
(s/def ::titles-by-author-response (s/nilable (s/coll-of ::single-title-response)))


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
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/register"
    {:post {:summary "Register a new user."
            :description ""
            :parameters {:body {:username string?
                                :email string?
                                :password string?
                                :confirm string?}}
            :responses {200 {:body {:message string?}}
                        400 {:body {:message string?}}
                        409 {:body {:message string?}}}
            :handler (fn [{{{:keys [username email password confirm]} :body} :parameters}]
                       (if-not (= password confirm)
                         (response/bad-request {:message "Password and Confirm do not match."})
                         (try
                           (auth/create-user! username email password)
                           (response/ok {:message "User registration successful. Please log in."})
                           (catch clojure.lang.ExceptionInfo e
                             (cond
                               (= (:cde/error-id (ex-data e)) :auth/duplicate-user-username)
                               (response/conflict {:message "Registration failed! A user with that username already exists!"})
                               (= (:cde/error-id (ex-data e)) :auth/duplicate-user-email)
                               (response/conflict {:message "Registration failed! A user with that email already exists!"}))
                             :else (throw e)))))}}]

   ["/login"
    {:post {:summary "Log in to the system."
            :description ""
            :parameters {:body {:email string?
                                :password string?}}
            :responses {200 {:body
                             {:identity {:email string? :created_at any?}}}
                        401 {:body {:message string?}}}
            :handler (fn [{{{:keys [email password]} :body} :parameters
                           session :session}]
                       (println "session: " session)
                       (if-some [user (auth/authenticate-user email password)]
                         (-> (response/ok
                              {:identity user})
                             (assoc :session (assoc session :identity user)))
                         (response/unauthorized
                          {:message "Invalid email or password"})))}}]

   ["/logout"
    {:post {:summary "Log out of the system and invalidate the session."
            :description ""
            :handler (fn [_] (-> (response/ok)
                                 (assoc :session nil)))}}]

   ["/platform/statistics"
    {:get {:summary "Get platform statistics: number of titles, authors, chapters, etc."
           :description ""
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

   ["/search/titles"
    {:get {:summary "Search for titles."
           :description ""
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

   ["/user/:id/profile"
    {:get {:summary "Get profile details of a single user."
           :description ""
           :parameters {:path {:id ::user-id}}
           :responses {200 {:body ::profile-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [user (auth/get-user-profile id)]
                        (response/ok user)
                        (response/not-found {:message "User profile not found"})))}}]

  ;;  ["/user/:id/collections"
  ;;   {:get {:summary "Get a list of all collections of a single user."
  ;;          :description ""
  ;;          :parameters {:path {:id ::user-id}}
  ;;          :responses {200 {:body ::collections-response}
  ;;                      404 {:body {:message string?}}}
  ;;          :handler (fn [{{{:keys [id]} :path} :parameters}]
  ;;                     (if-let [collections (collection/get-user-collections id)]
  ;;                       (response/ok collections)
  ;;                       (response/not-found {:message "User collections not found"})))}}]

  ;;  ["/user/:id/bookmarks"
  ;;   {:get {:summary "Get a list of all items bookmarked by a user, regardless of the bookmark collection the user has put them in."
  ;;          :description ""
  ;;          :parameters {:path {:id ::user-id}}
  ;;          :responses {200 {:body ::bookmarks-response}
  ;;                      404 {:body {:message string?}}}
  ;;          :handler (fn [{{{:keys [id]} :path} :parameters}]
  ;;                     (if-let [bookmarks (collection/get-all-user-collection-items id)]
  ;;                       (response/ok bookmarks)
  ;;                       (response/not-found {:message "User bookmarks not found"})))}}]

   ["/newspaper/:id"
    {:get {:summary "Get details of a single newspaper."
           :description ""
           :parameters {:path {:id ::newspaper-id}}
           :responses {200 {:body ::newspaper-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [newspaper (newspaper/get-newspaper id)]
                        (response/ok newspaper)
                        (response/not-found {:message "Newspaper not found"})))}}]

   ["/newspaper/:id/titles"
    {:get {:summary "Get a list of all titles published in a given newspaper."
           :description ""
           :parameters {:path {:id ::newspaper-id}}
           :responses {200 {:body ::chapters-within-title-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [titles (newspaper/get-titles-in-newspaper id)]
                        (response/ok titles)
                        (response/not-found {:message "No titles found"})))}}]

   ["/author/:id"
    {:get {:summary "Get details of a single author by id."
           :description ""
           :parameters {:path {:id ::author-id}}
           :responses {200 {:body ::author-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [author (author/get-author id)]
                        (response/ok author)
                        (response/not-found {:message "Author not found"})))}}]

   ["/author/:id/titles"
    {:get {:summary "Get a list of all titles by a single author (matched to that author's id)."
           :description ""
           :parameters {:path {:id ::author-id}}
           :responses {200 {:body ::titles-by-author-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [titles (author/get-titles-by-author id)]
                        (response/ok titles)
                        (response/not-found {:message "No titles found for that author."})))}}]

   ["/author-nationalities"
    {:get {:summary "Get a list of all nationalities currently listed in our authors records."
           :description ""
           :responses {200 {:body ::author-nationalities-response}
                       404 {:body {:message string?}}}
           :handler (fn [_]
                      (if-let [nationalities (author/get-nationalities)]
                        (response/ok nationalities)
                        (response/not-found {:message "No nationalities found"})))}}]

   ["/author-genders"
    {:get {:summary "Get a list of all genders currently listed in our authors records."
           :description ""
           :responses {200 {:body ::author-genders-response}
                       404 {:body {:message string?}}}
           :handler (fn [_]
                      (if-let [genders (author/get-genders)]
                        (response/ok genders)
                        (response/not-found {:message "No genders found"})))}}]

   ["/title/:id"
    {:get {:summary "Get details of a single title by id."
           :description ""
           :parameters {:path {:id ::title-id}}
           :responses {200 {:body ::single-title-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [title (title/get-title id true)]
                        (response/ok title)
                        (response/not-found {:message "Title not found"})))}}]

   ["/title/:id/chapters"
    {:get {:summary "Get a list of all chapters in a given title."
           :description ""
           :parameters {:path {:id ::title-id}}
           :responses {200 {:body ::chapters-within-title-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [chapters (chapter/get-chapters-in-title id)]
                        (response/ok chapters)
                        (response/not-found {:message "No chapters found"})))}}]

   ["/chapter/:id"
    {:get {:summary "Get details of a single chapter by id."
           :description ""
           :parameters {:path {:id ::chapter-id}}
           :responses {200 {:body ::single-chapter-response}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (if-let [chapter (chapter/get-chapter id)]
                        (response/ok chapter)
                        (response/not-found {:message "Chapter not found"})))}}]


   ["/create/newspaper"
    {:post {:summary "Create a new newspaper."
            :description ""
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
            :parameters {:body ::create-title-request}
            :responses {200 {:body {:message string? :id integer?}}
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
            :parameters {:body ::create-chapter-request}
            :responses {200 {:body {:message string? :id integer?}}
                        400 {:body {:message string? :details any?}}}
            :handler (fn [{:keys [parameters]}]
                       (let [body (:body parameters)]
                         (try
                           (let [id (chapter/create-chapter! body)]
                             (response/ok {:message "Chapter creation successful." :id id}))
                           (catch Exception e
                             (response/bad-request {:message (str "Chapter creation failed: " (.getMessage e))
                                                    :details e})))))}}]])