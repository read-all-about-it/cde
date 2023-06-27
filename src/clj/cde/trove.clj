(ns cde.trove
  "Code for interfacing with the Trove API"
  (:require
   [cde.config :refer [env]]
   [org.httpkit.client :as http]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.edn :as edn]))


;; Code for interfacing with the Trove API

(def trove-api-url "https://api.trove.nla.gov.au/v3") ;; the base URL for the Trove API

(def trove-api-key (first (get-in env [:trove-api-keys]))) ;; the Trove API key

(defn- trove-endpoint
  "Return the full URL for the given endpoint, given a set of parameters."
  [endpoint params]
  (str trove-api-url endpoint
       "?key=" trove-api-key
       "&encoding=json"
       (apply str (for [[k v] params] (str "&" (name k) "=" v)))))

(defn trove-newspaper-url
  "Return the Trove API url for a newspaper with a given id."
  [id]
  (trove-endpoint (str "/newspaper/title/" id) {}))

(defn trove-article-url
  "Return the Trove API url for an article with the given id.
   Ensures that the endpoint returns the full article text, not just the metadata."
  [id]
  (trove-endpoint (str "/newspaper/" id)
                  {:reclevel "full"
                   :include "articletext"}))


(defn- trove-get
  "Synchronously get the response from the given Trove API endpoint.
   
   Coerces the response into a form more useful for our purposes."
  [endpoint]
  (let [response @(http/get endpoint)
        body (-> response :body (json/read-str :key-fn keyword))
        status (-> response :status)]
    {:body body
     :trove_api_status status
     :trove_endpoint endpoint}))

(defn trove-newspaper->tbc-newspaper
  "Take a newspaper JSON response (ie, from trove-get on a newspaper endpoint)
   and return a newspaper in the format used by the TBC platform."
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
  "Take an article JSON response (ie, from trove-get on an article endpoint)
   and return a chapter in the format used by the TBC platform."
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
  "Return the JSON response for a newspaper with the given id."
  [id]
  (trove-newspaper->tbc-newspaper (trove-get (trove-newspaper-url id))))

(defn get-article
  "Return the JSON response for an article with the given id."
  [id]
  (trove-article->tbc-chapter (trove-get (trove-article-url id))))