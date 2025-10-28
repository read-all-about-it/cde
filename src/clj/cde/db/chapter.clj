(ns cde.db.chapter
  "Chapter entity CRUD operations with Trove API integration.

   Provides functions for creating, reading, updating, and listing chapters.
   Chapters represent individual instalments of a serialised fiction title,
   corresponding to newspaper articles in the Trove archive.

   Key fields:
   - title_id: Foreign key to title (required)
   - trove_article_id: Unique identifier from Trove API (required)
   - chapter_number: Sequence number within the title
   - chapter_html/chapter_text: Article content (synced from Trove)
   - final_date: Publication date

   Trove integration:
   - create-chapter! auto-fetches article data from Trove API
   - update-chapter-from-trove! refreshes content from Trove"
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [nil-fill-default-params html->txt drop-nil-params date? parse-date]]
   [cde.trove :as trove]
   [clojure.string :as str]))

(def ^:private updateable-chapter-keys
  [:chapter_number
   :chapter_title
   :article_url
   :dow
   :pub_day
   :pub_month
   :pub_year
   :final_date
   :page_references
   :page_url
   :corrections
   :word_count
   :illustrated
   :last_corrected
   :page_sequence
   :chapter_html
   :chapter_text
   :text_title
   :export_title])

(def ^:private defer-to-trove-keys
  "Keys that should always be taken from Trove when updating from the API."
  [:chapter_html
   :chapter_text
   :article_url
   :corrections
   :word_count
   :illustrated])

(defn- parse-final-date
  "Parses the final_date field in params if present."
  [params]
  (let [final-date (:final_date params)
        parsed-date (parse-date final-date)]
    (-> params
        (assoc :final_date (if (date? parsed-date) parsed-date nil)))))

(defn- fill-chapter-text-param
  "Converts chapter_html to plain text and stores in chapter_text if not already set.
   Uses Jsoup to strip HTML tags while preserving text content."
  [params]
  (if (nil? (:chapter_text params))
    (assoc params :chapter_text (html->txt (:chapter_html params)))
    params))

(defn- fill-params-from-trove
  "Fetches article details from Trove API and merges into params.
   Only fills in values for keys that are not already present in params."
  [params]
  (let [trove-details (dissoc (trove/get-article (:trove_article_id params))
                              :trove_api_status :trove_newspaper_id :chapter_title)]
    (if (:chapter_html trove-details) ;; looks like we have a chapter_html, and so have some valid trove details
      ;; merge the trove details into the params, but only if the params don't already have a value for that key
      (reduce (fn [params [k v]]
                (if (nil? (params k))
                  (assoc params k v)
                  params))
              params
              trove-details)
      params)))

(defn- fix-final-date-param
  "Ensures final_date is a valid LocalDate.
   If final_date is blank but pub_day/pub_month/pub_year are present,
   constructs the date from those components."
  [params]
  (let [zero-pad (fn [s] (if (< (count s) 2) (str "0" s) s))]
    (cond (and (str/blank? (:final_date params)) (:pub_day params) (:pub_month params) (:pub_year params))
          (let [final-date (parse-date (str (zero-pad (str (:pub_year params)))
                                            "-"
                                            (zero-pad (str (:pub_month params)))
                                            "-"
                                            (zero-pad (str (:pub_day params)))))]
            (assoc params :final_date final-date))
          (string? (:final_date params))
          (assoc params :final_date (parse-date (:final_date params)))
          :else params)))

(defn create-chapter!
  "Creates a new chapter record in the database.

   Required: :title_id, :trove_article_id
   Optional: :chapter_number, :chapter_title, :article_url, :dow, :pub_day,
             :pub_month, :pub_year, :final_date, and more.

   Automatically fetches article content from Trove API if not provided.
   Returns the ID of the newly created chapter."
  [params]
  (let [missing (filter #(nil? (params %)) [:title_id :trove_article_id])
        optional-keys [:chapter_number
                       :chapter_title
                       :article_url
                       :dow
                       :pub_day
                       :pub_month
                       :pub_year
                       :final_date
                       :page_references
                       :page_url
                       :word_count
                       :illustrated
                       :page_sequence
                       :chapter_html
                       :chapter_text
                       :text_title
                       :export_title
                       :added_by]]
    (if (not (empty? missing))
      (throw (ex-info (apply str "Missing required parameters: " (interpose " " missing))
                      {:cde/error-id ::missing-required-params
                       :error (apply str "Missing required parameters: " (interpose " " missing))
                       :missing missing}))
      ; if no missing required params, continue, but check that the Title ID actually matches a title
      (let [matching-title (db/get-title-by-id* {:id (:title_id params)})]
        (if (empty? matching-title)
          (throw (ex-info (str "No title found with id " (:title_id params) " (necessary to match for chapter creation)")
                          {:cde/error-id ::no-matching-title-for-chapter
                           :error (str "No title found with id " (:title_id params))}))
          (try
            (as-> params p
              (nil-fill-default-params optional-keys p)
              (dissoc p :chapter_html :chapter_text)
              (fill-params-from-trove p)
              (fill-chapter-text-param p)
              (fix-final-date-param p)
                ;;  (parse-final-date p)
              (db/create-chapter!* p)
              (:id p)) ;; get id of the inserted chapter (if successful)
            (catch Exception e
              (throw (ex-info "Error creating chapter"
                              {:cde/error-id ::create-chapter-exception
                               :error (.getMessage e)})))))))))

(defn get-chapter
  "Fetches a chapter by its database ID.
   Excludes large text vector and some internal fields from the response."
  [id]
  (let [chapter (db/get-chapter-by-id* {:id id})]
    (if (empty? chapter)
      (throw (ex-info "No chapter found with that ID!"
                      {:cde/error-id ::no-chapter-found
                       :error "No chapter found with ID!"}))
      (-> chapter
          (dissoc :chapter_text_vector
                  :export_title
                  :dow
                  :pub_day
                  :pub_month
                  :pub_year
                  :output)))))

(defn get-chapters-in-title
  "Fetches all chapters belonging to a specific title, ordered by final_date."
  [title-id]
  (let [chapters (db/get-all-chapters-in-title* {:title_id title-id})]
    (if (empty? chapters)
      (throw (ex-info "No chapters found for that title!"
                      {:cde/error-id ::no-chapters-found
                       :error "No chapters found for that title!"}))
      chapters)))

(defn trove-article-id->chapter-id
  "Gets the chapter id from the database that matches a record
   with a given Trove Article ID. Returns nil if no matching chapter is found
   in our database."
  [trove-article-id]
  ;; {:pre [(integer? trove-article-id)]}
  (let [chapter (db/get-chapter-by-trove-article-id* {:trove_article_id trove-article-id})]
    (if (empty? chapter)
      nil
      (:id (first chapter)))))

(defn update-chapter!
  "Updates an existing chapter record by its database ID.

   Merges new-params with existing values, only updating fields that are
   present in new-params. Regenerates chapter_text from chapter_html if needed."
  [id new-params]
  {:pre [(number? id) (map? new-params)]}
  (jdbc/with-transaction [t-conn db/*db*]
    (let [existing-chapter (db/get-chapter-by-id* {:id id})]
      (when (empty? existing-chapter)
        (throw (ex-info "No chapter found with that ID!"
                        {:cde/error-id ::no-chapter-found
                         :error "No chapter found with ID!"})))
      (let [clean-params (-> new-params (parse-final-date) (drop-nil-params))]
        (when (empty? clean-params)
          (throw (ex-info "No valid parameters provided for update!"
                          {:cde/error-id ::no-valid-params
                           :error "No valid parameters provided for update!"})))
        (let [chapter-for-update (-> existing-chapter
                                     (dissoc :chapter_text_vector)
                                     (merge clean-params)
                                     (select-keys updateable-chapter-keys)
                                     (fill-chapter-text-param)
                                     (assoc :id id))]
          (try (db/update-chapter!* t-conn chapter-for-update)
               (catch Exception e
                 (throw (ex-info "Error updating chapter"
                                 {:cde/error-id ::update-chapter-exception
                                  :error (.getMessage e)})))))))))

(defn update-chapter-from-trove!
  "Refreshes a chapter's content from the Trove API.

   Finds the chapter by its trove_article_id, fetches the latest article data
   from Trove, and updates the chapter with content fields (chapter_html,
   chapter_text, article_url, corrections, word_count, illustrated).

   Returns the updated chapter record."
  [trove-article-id]
  (let [chapter (db/get-chapter-by-trove-article-id* {:trove_article_id trove-article-id})]
    (if (empty? chapter)
      (throw (ex-info "No chapter found with that Trove Article ID!"
                      {:cde/error-id ::no-chapter-found
                       :error "No chapter found with that Trove Article ID!"}))
      (let [chapter-data (first chapter)
            id (:id chapter-data)
            trove-data (trove/get-article trove-article-id)]
        (when (not= 200 (:trove_api_status trove-data))
          (throw (ex-info (str "Trove API returned error status: " (:trove_api_status trove-data))
                          {:cde/error-id ::trove-api-error
                           :error (str "Trove API error: " (:trove_api_status trove-data))
                           :trove_api_status (:trove_api_status trove-data)})))
        (let [update-params (select-keys trove-data defer-to-trove-keys)]
          (update-chapter! id update-params)
          (get-chapter id))))))

(defn get-chapters
  "Get an unfiltered list of chapters from the db.

   Accepts optional limit & offset params (defaulting to 50 & 0 respectively).
   Limit is capped at 500 for performance reasons.

   Returns a map containing a list of chapters, along with next/previous links."
  ([]
   (get-chapters 50 0))
  ([limit]
   (get-chapters limit 0))
  ([limit offset]
   (let [limit (min limit 500)
         chapters (db/get-chapters* {:limit limit :offset offset})
         next (if (= limit (count chapters))
                (str "/chapters?limit=" limit "&offset=" (+ offset limit))
                nil)
         prev (if (> offset 0)
                (str "/chapters?limit=" limit "&offset=" (max (- offset limit) 0))
                nil)]
     {:results (map #(dissoc % :chapter_text :chapter_text_vector) chapters) ;; remove the big text fields from the results
      :next next
      :previous prev})))
