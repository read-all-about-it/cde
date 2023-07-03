(ns cde.db.chapter
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [java-time.api :as jt]
   [cde.utils :refer [nil-fill-default-params html->txt drop-nil-params]]
   [cde.trove :as trove]
   [clojure.string :as str]
   [cde.db.chapter :as chapter]))

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

(def ^:private defer-to-trove-keys ;; always take these from trove if they are available when updating
  [:chapter_html
   :chapter_text
   :corrections
   :last_corrected
   :word_count
   :illustrated])


(defn- date? [s]
  (if (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    true
    false))

(defn- parse-date [s]
  (jt/local-date "yyyy-MM-dd" s))

(defn- parse-final-date [params]
  (let [final-date (:final_date params)]
    (if final-date
      (if (date? final-date)
        (assoc params :final_date (parse-date final-date))
        (throw (ex-info "Invalid date format" {:cde/error-id ::invalid-date-format
                                               :error "Date must be in the format YYYY-MM-DD"})))
      params)))

(defn- fill-chapter-text-param
  "Convert chapter_html to chapter_text (ie, take html string; convert to text)
   if the chapter_text param is nil"
  [params]
  (if (nil? (:chapter_text params))
    (assoc params :chapter_text (html->txt (:chapter_html params)))
    params))

(defn- fill-params-from-trove
  "Get available chapter details from trove article and fill in params"
  [params]
  (let [trove-details (dissoc (trove/get-article (:trove_article_id params))
                              :trove_api_status :trove_newspaper_id)]
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
  "Take a chapter map for update/creation. If the final_date is a string, parse it; if it's nill, create a string from the other date params and parse that."
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

(defn create-chapter! [params]
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
          (do (println "Issue with chapter input - no title found with id " (:title_id params))
              (throw (ex-info (str "No title found with id " (:title_id params) "(necessary to match for chapter creation)")
                              {:cde/error-id ::no-matching-title-for-chapter
                               :error (str "No title found with id " (:title_id params))})))
          (try
            (->> params
                 (parse-final-date)
                 (nil-fill-default-params optional-keys)
                 (fill-params-from-trove)
                 (fill-chapter-text-param)
                 (fix-final-date-param)
                 (db/create-chapter!*)
                 (:id)) ;; get id of the inserted chapter (if successful)
            (catch Exception e
              (do (println e)
                  (throw (ex-info "Error creating chapter"
                                  {:cde/error-id ::create-chapter-exception
                                   :error (.getMessage e)}))))))))))


(defn get-chapter
  [id]
  (let [chapter (db/get-chapter-by-id* {:id id})]
    (if (empty? chapter)
      (throw (ex-info "No chapter found with that ID!"
                      {:cde/error-id ::no-chapter-found
                       :error "No chapter found with ID!"}))
      chapter)))


(defn get-chapters-in-title [title-id]
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
  "Update the values of a chapter by its primary key (id)."
  [id new-params]
  {:pre [(number? id) (map? new-params)]}
  (jdbc/with-transaction [t-conn db/*db*]
    (let [existing-chapter (get-chapter id)
          clean-params (-> new-params (parse-final-date) (drop-nil-params))
          chapter-for-update (-> existing-chapter
                               (merge clean-params)
                               (select-keys updateable-chapter-keys)
                               (assoc :id id))]
      (println "existing-chapter: " existing-chapter)
      (println "chapter-for-update: " chapter-for-update)
      (cond (empty? existing-chapter)
            (throw (ex-info "No chapter found with that ID!"
                            {:cde/error-id ::no-chapter-found
                             :error "No chapter found with ID!"}))
            (empty? clean-params)
            (throw (ex-info "No valid parameters provided for update!"
                            {:cde/error-id ::no-valid-params
                             :error "No valid parameters provided for update!"}))
            :else
            (try (db/update-chapter!* t-conn chapter-for-update)
                 (catch Exception e
                   (throw (ex-info "Error updating chapter"
                                   {:cde/error-id ::update-chapter-exception
                                    :error (.getMessage e)}))))))))


(defn update-chapter-from-trove!
  "Get a chapter from the database that matches a record with a given Trove Article ID.
   Update the chapter with the latest details from Trove."
  [trove-article-id]
  (let [chapter (db/get-chapter-by-trove-article-id* {:trove_article_id trove-article-id})]
    (if (empty? chapter)
      (throw (ex-info "No chapter found with that Trove Article ID!"
                      {:cde/error-id ::no-chapter-found
                       :error "No chapter found with that Trove Article ID!"}))
      (let [updated-chapter (as-> chapter x
                              (first x)
                              (apply (partial dissoc x) defer-to-trove-keys)
                              (fill-params-from-trove x))
            id (:id (first chapter))]
        (update-chapter! id updated-chapter)))))