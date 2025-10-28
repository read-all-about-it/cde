(ns cde.trove
  "Integration with the National Library of Australia's Trove API.

  Provides functions for fetching newspaper and article data from Trove
  and converting it to the TBC platform's internal format.

  Key functions:
  - [[get-newspaper]]: Fetch newspaper metadata by Trove ID
  - [[get-article]]: Fetch article/chapter content by Trove ID

  Configuration:
  - `:trove-api-keys` in env - Vector of API keys for rotation

  See also: [[cde.routes.trove]] for HTTP endpoints"
  (:require
   [cde.config :refer [env]]
   [org.httpkit.client :as http]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.edn :as edn]))

(def trove-api-url
  "Base URL for the Trove API v3."
  "https://api.trove.nla.gov.au/v3")

(defn- ^:no-doc get-trove-api-key
  "Selects a random Trove API key from configured keys.

  Enables load distribution across multiple API keys to avoid rate limits.

  Returns: API key string, or nil if no keys configured."
  []
  (let [keys (get-in env [:trove-api-keys])]
    (when (and keys (seq keys))
      (rand-nth keys))))

(defn- ^:no-doc trove-endpoint
  "Constructs a full Trove API URL with authentication and parameters.

  Arguments:
  - `endpoint` - API endpoint path (e.g., \"/newspaper/title/123\")
  - `params` - Map of query parameters to append

  Returns: Complete URL string with API key and JSON encoding."
  [endpoint params]
  (let [api-key (get-trove-api-key)]
    (str trove-api-url endpoint
         "?key=" api-key
         "&encoding=json"
         (apply str (for [[k v] params] (str "&" (name k) "=" v))))))

(defn trove-newspaper-url
  "Returns the Trove API URL for fetching newspaper metadata.

  Arguments:
  - `id` - Trove newspaper ID (integer or string)

  Returns: Complete API URL string."
  [id]
  (trove-endpoint (str "/newspaper/title/" id) {}))

(defn trove-article-url
  "Returns the Trove API URL for fetching article content.

  Configures the endpoint to return full article text, not just metadata.
  Uses `reclevel=full` and `include=articletext` parameters.

  Arguments:
  - `id` - Trove article ID (integer or string)

  Returns: Complete API URL string."
  [id]
  (trove-endpoint (str "/newspaper/" id)
                  {:reclevel "full"
                   :include "articletext"}))

(defn- ^:no-doc trove-get
  "Performs a synchronous GET request to a Trove API endpoint.

  Blocks until response is received using http-kit's promise deref.

  Arguments:
  - `endpoint` - Complete Trove API URL

  Returns: Map with keys:
  - `:body` - Parsed JSON response as keywords
  - `:trove_api_status` - HTTP status code
  - `:trove_endpoint` - Original endpoint URL (for debugging)"
  [endpoint]
  (let [response @(http/get endpoint)
        body (-> response :body (json/read-str :key-fn keyword))
        status (-> response :status)]
    {:body body
     :trove_api_status status
     :trove_endpoint endpoint}))

(defn trove-newspaper->tbc-newspaper
  "Transforms a Trove newspaper response to TBC platform format.

  Extracts and normalises newspaper metadata from Trove's JSON format
  into the structure expected by the TBC database.

  Arguments:
  - `trove-newspaper` - Response map from [[trove-get]] on a newspaper endpoint

  Returns: Map with keys matching TBC newspaper schema:
  - `:trove_newspaper_id` - Trove's newspaper ID (integer)
  - `:title` - Newspaper title
  - `:colony_state` - State/territory code
  - `:start_date` / `:end_date` - Publication date range (strings)
  - `:start_year` / `:end_year` - Extracted years (integers)
  - `:issn` - ISSN if available
  - `:trove_api_status` - HTTP status from fetch"
  [trove-newspaper]
  {:pre [(map? trove-newspaper)]
   :post [(map? %)]}
  {:trove_newspaper_id (edn/read-string (get-in trove-newspaper [:body :id]))
   :title (get-in trove-newspaper [:body :title])
   :colony_state (get-in trove-newspaper [:body :state])
   :start_date (get-in trove-newspaper [:body :startDate])
   :end_date (get-in trove-newspaper [:body :endDate])
   :issn (get-in trove-newspaper [:body :issn])
   :start_year (-> (get-in trove-newspaper [:body :startDate] "")
                   (str/split #"-")
                   (first)
                   (edn/read-string))
   :end_year (-> (get-in trove-newspaper [:body :endDate] "")
                 (str/split #"-")
                 (first)
                 (edn/read-string))
   :trove_api_status (get-in trove-newspaper [:trove_api_status])})

(defn trove-article->tbc-chapter
  "Transforms a Trove article response to TBC chapter format.

  Extracts and normalises article data from Trove's JSON format.
  Attempts to parse chapter numbers from headings using Roman numeral detection.

  Arguments:
  - `trove-article` - Response map from [[trove-get]] on an article endpoint

  Returns: Map with keys matching TBC chapter schema:
  - `:trove_article_id` - Trove's article ID (integer)
  - `:chapter_title` - Article heading
  - `:chapter_number` - Extracted Roman numerals from heading (or nil)
  - `:article_url` / `:page_url` - Trove URLs for the article
  - `:final_date` - Publication date
  - `:page_number` / `:page_sequence` - Page information
  - `:word_count` / `:corrections` - Article statistics
  - `:illustrated` - Boolean if article has illustrations
  - `:chapter_html` - Full article text as HTML
  - `:trove_newspaper_id` - Parent newspaper's Trove ID
  - `:trove_api_status` - HTTP status from fetch"
  [trove-article]
  {:pre [(map? trove-article)]
   :post [(map? %)]}
  (let [illustrated-to-boolean (fn [illustrated]
                                 (cond (= illustrated "Y") true
                                       (= illustrated "N") false
                                       :else nil))
        roman-numerals-regex "M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})"]
    {:trove_article_id (edn/read-string (get-in trove-article [:body :id]))
     :chapter_title (get-in trove-article [:body :heading] nil)
     :chapter_number (if (not (get-in trove-article [:body :heading]))
                       nil
                       (as-> (get-in trove-article [:body :heading]) x
                          ;; replace all punctuation with spaces
                         (str/replace x #"[.,;-?!\[\]\(\)\}\{]" " ")
                          ;; split on spaces
                         (str/split x #"\s+")
                          ;; remove empty strings
                         (remove empty? x)
                          ;; get any strings that match the roman numeral regex
                         (filter #(re-matches (re-pattern roman-numerals-regex) %) x)
                       ;; join the matches together with '-'
                         (str/join "-" x)))
     :article_url (get-in trove-article [:body :troveUrl] nil)
     :final_date (get-in trove-article [:body :date] nil)
     :page_number (edn/read-string (get-in trove-article [:body :page] nil))
     :page_url (get-in trove-article [:body :trovePageUrl])
     :corrections (edn/read-string (get-in trove-article [:body :correctionCount]))
     :word_count (edn/read-string (get-in trove-article [:body :wordCount]))
     :illustrated (illustrated-to-boolean
                   (get-in trove-article [:body :illustrated]))
     :last_corrected (first (str/split (get-in trove-article [:body :lastCorrection :lastupdated] "") #"T"))
     :page_sequence (get-in trove-article [:body :pageSequence])
     :chapter_html (get-in trove-article [:body :articleText])
     :trove_newspaper_id (edn/read-string (get-in trove-article [:body :title :id]))
     :trove_api_status (get-in trove-article [:trove_api_status])}))

(defn get-newspaper
  "Fetches and transforms newspaper metadata from Trove.

  Convenience function that combines URL construction, API fetch,
  and format transformation.

  Arguments:
  - `id` - Trove newspaper ID

  Returns: TBC-formatted newspaper map.

  See also: [[trove-newspaper->tbc-newspaper]]"
  [id]
  (trove-newspaper->tbc-newspaper (trove-get (trove-newspaper-url id))))

(defn get-article
  "Fetches and transforms article content from Trove.

  Convenience function that combines URL construction, API fetch,
  and format transformation. Includes full article text.

  Arguments:
  - `id` - Trove article ID

  Returns: TBC-formatted chapter map with article content.

  See also: [[trove-article->tbc-chapter]]"
  [id]
  (trove-article->tbc-chapter (trove-get (trove-article-url id))))
