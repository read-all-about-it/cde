(ns cde.db.search
  "Full-text search operations for titles and chapters.

  Provides search functions using PostgreSQL ILIKE for substring matching.
  Each search parameter is optional - when nil or blank, that filter is skipped.

  Search features:
  - Multi-field filtering (title, author, newspaper, nationality)
  - Case-insensitive substring matching via ILIKE
  - Pagination via limit/offset

  Key functions: [[search-titles]], [[search-chapters]]."
  (:require
   [cde.db.core :as db]
   [clojure.string :as str]
   [clojure.tools.logging :as log]))

;;;; Private Helpers

(defn- ^:no-doc set-limit-offset-defaults
  "Sets default pagination values if not provided.

  Arguments:
  - `params` - Query parameters map

  Returns: Map with `:limit` (default 50) and `:offset` (default 0) set."
  [params]
  (let [limit (or (:limit params) 50)
        offset (or (:offset params) 0)]
    (assoc params :limit limit :offset offset)))

(defn- ^:no-doc build-pagination-url
  "Builds a pagination URL with query parameters.

  Constructs a URL string with all non-nil search parameters preserved,
  updating limit and offset for pagination navigation.

  Arguments:
  - `base-path` - The API endpoint path (e.g., \"/search/titles\")
  - `params` - Map of query parameters (may include nil values)
  - `limit` - Results per page
  - `offset` - New offset value for the link

  Returns: URL string like \"/search/titles?title_text=foo&limit=50&offset=100\"."
  [base-path params limit offset]
  (let [;; Filter out nil/empty values and internal keys
        clean-params (into {}
                           (filter (fn [[_ v]]
                                     (and (some? v)
                                          (not (and (string? v) (str/blank? v)))))
                                   params))
        ;; Build query string with pagination
        query-params (-> clean-params
                         (assoc :limit limit :offset offset))
        query-string (->> query-params
                          (map (fn [[k v]] (str (name k) "=" (java.net.URLEncoder/encode (str v) "UTF-8"))))
                          (str/join "&"))]
    (str base-path "?" query-string)))

(defn- ^:no-doc build-pagination-links
  "Builds next and previous pagination links for search results.

  Arguments:
  - `base-path` - The API endpoint path
  - `original-params` - Original search parameters (before normalization)
  - `results` - Search results collection
  - `limit` - Results per page
  - `offset` - Current offset

  Returns: Map with `:next` and `:previous` keys (values may be nil)."
  [base-path original-params results limit offset]
  (let [;; Select only the search-relevant params (exclude internal ones)
        search-params (select-keys original-params
                                   [:title_text :newspaper_title_text :author_nationality
                                    :author_name :chapter_text])
        next-link (when (= limit (count results))
                    (build-pagination-url base-path search-params limit (+ offset limit)))
        prev-link (when (> offset 0)
                    (build-pagination-url base-path search-params limit (max 0 (- offset limit))))]
    {:next next-link
     :previous prev-link}))

(defn- ^:no-doc normalize-search-param
  "Normalizes a search parameter value.

  Converts empty/blank strings to nil so SQL optional filters work correctly.
  Trims whitespace from valid strings.

  Arguments:
  - `value` - Search parameter value (string or nil)

  Returns: Trimmed string, or nil if input is blank/empty."
  [value]
  (when (and value (string? value) (not (str/blank? value)))
    (str/trim value)))

(defn- ^:no-doc normalize-search-params
  "Normalizes all search parameters in a map.

  Applies [[normalize-search-param]] to specified keys, converting
  blank strings to nil for proper SQL optional filter handling.

  Arguments:
  - `params` - Map of search parameters
  - `keys-to-normalize` - Collection of keys to normalize

  Returns: Map with normalized string values."
  [params keys-to-normalize]
  (reduce (fn [m k]
            (update m k normalize-search-param))
          params
          keys-to-normalize))

;;;; Public API

(defn search-titles
  "Searches titles with optional filters on multiple fields.

  Uses PostgreSQL ILIKE for case-insensitive substring matching.
  Each filter parameter is optional - when nil or blank, that filter is skipped,
  returning all titles that match the remaining criteria.

  Arguments:
  - `query-params` - Map with optional keys:
    - `:title_text` - Search within title names (common_title, publication_title)
    - `:newspaper_title_text` - Filter by newspaper name
    - `:author_nationality` - Filter by author nationality
    - `:author_name` - Filter by author name (common_name, other_name, attributed)
    - `:limit` - Max results (default 50)
    - `:offset` - Pagination offset (default 0)

  Returns: Map with:
  - `:results` - Vector of matching title records with joined author/newspaper data
  - `:limit` - Results per page
  - `:offset` - Current offset
  - `:search_type` - Always \"title\"
  - `:next` - URL for next page (nil if no more results)
  - `:previous` - URL for previous page (nil if on first page)"
  [query-params]
  (let [search-keys [:title_text :newspaper_title_text :author_nationality :author_name]
        params-with-defaults (set-limit-offset-defaults query-params)
        clean-params (-> params-with-defaults
                         (select-keys (concat search-keys [:limit :offset]))
                         (normalize-search-params search-keys))
        _ (log/debug "Search for titles:" clean-params)
        search-results (db/search-titles* clean-params)
        pagination (build-pagination-links "/search/titles"
                                           clean-params
                                           search-results
                                           (:limit clean-params)
                                           (:offset clean-params))]
    {:results search-results
     :limit (:limit clean-params)
     :offset (:offset clean-params)
     :search_type "title"
     :next (:next pagination)
     :previous (:previous pagination)}))

(defn search-chapters
  "Searches chapters with optional filters on multiple fields.

  Uses PostgreSQL ILIKE for case-insensitive substring matching.
  Each filter parameter is optional - when nil or blank, that filter is skipped,
  returning all chapters that match the remaining criteria.

  Arguments:
  - `query-params` - Map with optional keys:
    - `:chapter_text` - Search within chapter content
    - `:title_text` - Filter by title name
    - `:newspaper_title_text` - Filter by newspaper name
    - `:author_nationality` - Filter by author nationality
    - `:author_name` - Filter by author name
    - `:limit` - Max results (default 50)
    - `:offset` - Pagination offset (default 0)

  Returns: Map with:
  - `:results` - Vector of matching chapter records with joined title/author/newspaper data
  - `:limit` - Results per page
  - `:offset` - Current offset
  - `:search_type` - Always \"chapter\"
  - `:next` - URL for next page (nil if no more results)
  - `:previous` - URL for previous page (nil if on first page)"
  [query-params]
  (let [search-keys [:chapter_text :title_text :newspaper_title_text :author_nationality :author_name]
        params-with-defaults (set-limit-offset-defaults query-params)
        clean-params (-> params-with-defaults
                         (select-keys (concat search-keys [:limit :offset]))
                         (normalize-search-params search-keys))
        _ (log/debug "Search for chapters:" clean-params)
        search-results (db/search-chapters* clean-params)
        pagination (build-pagination-links "/search/chapters"
                                           clean-params
                                           search-results
                                           (:limit clean-params)
                                           (:offset clean-params))]
    {:results search-results
     :limit (:limit clean-params)
     :offset (:offset clean-params)
     :search_type "chapter"
     :next (:next pagination)
     :previous (:previous pagination)}))
