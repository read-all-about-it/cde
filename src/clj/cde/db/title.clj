(ns cde.db.title
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]
   [java-time.api :as jt]))

(defn- date? [s]
  (if (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    true
    false))

(defn- parse-date [s]
  (jt/local-date "yyyy-MM-dd" s))

(defn- parse-span-dates [params]
  (let [span-start (:span-start params)
        span-end (:span-end params)]
    (if (and span-start span-end)
      (if (and (date? span-start) (date? span-end))
        (assoc params
               :span-start (parse-date span-start)
               :span-end (parse-date span-end))
        (throw (ex-info "Invalid date format" {:cde/error-id ::invalid-date-format
                                               :error "Date must be in the format YYYY-MM-DD"})))
      params)))

(defn create-title! [params]
  (let [missing (filter #(nil? (params %)) [:newspaper-table-id :author-id])
        optional-keys [:span-start :span-end :publication-title
                       :attributed-author-name :common-title :author-of
                       :additional-info :inscribed-author-nationality
                       :inscribed-author-gender :information-source
                       :length :trove-source :also-published :name-category
                       :curated-dataset :added-by]]
    (if (empty? missing)
      (jdbc/with-transaction [t-conn db/*db*]
        (try
          (->> params
               (parse-span-dates)
               (nil-fill-default-params optional-keys)
               (kebab->snake)
               (db/create-title!* t-conn)
               (:id)) ;; get id of the inserted title (if successful)
          (catch Exception e
            (throw (ex-info "Error creating title"
                            {:cde/error-id ::create-title-exception
                             :error (.getMessage e)})))))
      (throw (ex-info (apply str "Missing required parameters: " (interpose " " missing))
                      {:cde/error-id ::missing-required-params
                       :error (apply str "Missing required parameters: " (interpose " " missing))
                       :missing missing})))))


(defn get-title
  "Select & return a title by its primary key (id).
   Optionally, join author and newspaper tables to get more info."
  ([id join?]
   (if join?
     (let [title (db/get-title-by-id-with-author-newspaper-names* {:id id})]
       (if (empty? title)
         (throw (ex-info "No title found with that ID!"
                         {:cde/error-id ::no-title-found
                          :error "No title found with ID!"}))
         title))
     (get-title id)))
  ([id]
   (let [title (db/get-title-by-id* {:id id})]
     (if (empty? title)
       (throw (ex-info "No title found with that ID!"
                       {:cde/error-id ::no-title-found
                        :error "No title found with ID!"}))
       title))))


(defn get-terse-title-list
  "Get a 'terse' list of all titles, ordered by publication title.
   Only return: id, publication_title, common_title."
  []
  (let [titles (db/get-terse-title-list* {})]
    (if (empty? titles)
      (throw (ex-info "No titles found!"
                      {:cde/error-id ::no-titles-found
                       :error "No titles found!"}))
      titles)))