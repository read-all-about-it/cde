(ns cde.chapter
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [java-time.api :as jt]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]))

(defn- date? [s]
  (if (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    true
    false))

(defn- parse-date [s]
  (jt/local-date "yyyy-MM-dd" s))

(defn- parse-final-date [params]
  (let [final-date (:final-date params)]
    (if final-date
      (if (date? final-date)
        (assoc params :final-date (parse-date final-date))
        (throw (ex-info "Invalid date format" {:cde/error-id ::invalid-date-format
                                               :error "Date must be in the format YYYY-MM-DD"})))
      params)))

(defn create-chapter! [params]
  (let [missing (filter #(nil? (params %)) [:title-id])
        optional-keys [:trove-article-id
                       :chapter-number
                       :chapter-title
                       :article-url
                       :dow
                       :day
                       :month
                       :year
                       :final-date
                       :page-references
                       :page-url
                       :word-count
                       :illustrated
                       :page-sequence
                       :chapter-html
                       :chapter-text
                       :text-title
                       :export-title
                       :added-by]]
    (if (empty? missing)
      (jdbc/with-transaction [t-conn db/*db*]
        (try
          (->> params
               (parse-final-date)
               (nil-fill-default-params optional-keys)
               (kebab->snake)
               (db/create-chapter!* t-conn)
               (:id)) ;; get id of the inserted chapter (if successful)
          (catch Exception e
            (throw (ex-info "Error creating chapter"
                            {:cde/error-id ::create-chapter-exception
                             :error (.getMessage e)})))))
      (throw (ex-info (apply str "Missing required parameters: " (interpose " " missing))
                      {:cde/error-id ::missing-required-params
                       :error (apply str "Missing required parameters: " (interpose " " missing))
                       :missing missing})))))