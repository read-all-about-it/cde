(ns cde.db.title
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [nil-fill-default-params drop-nil-params drop-blank-params]]
   [java-time.api :as jt]
   [clojure.string :as str]))

(def ^:private updateable-title-keys
  [:newspaper_table_id
   :span_start
   :span_end
   :publication_title
   :attributed_author_name
   :common_title
   :author_id
   :author_of
   :additional_info
   :inscribed_author_nationality
   :inscribed_author_gender
   :information_source
   :length
   :trove_source
   :also_published
   :name_category
   :curated_dataset])


(defn- date? [s] 
  (cond 
    (nil? s) false
    (string? s) (re-matches #"^\d{4}-\d{2}-\d{2}$" s)
    (instance? java.time.LocalDate s) true
    :else false))

(defn- parse-date [s]
  (cond (and (string? s) (not (str/blank? s)) (re-matches #"^\d{4}-\d{2}-\d{2}$" s))
        (jt/local-date "yyyy-MM-dd" s)
        (instance? java.time.LocalDate s) s
        :else nil))

(defn- parse-span-dates [params]
  (let [span-start (:span_start params)
        span-end (:span_end params)
        parsed-span-start (parse-date span-start)
        parsed-span-end (parse-date span-end)]
    (-> params
        ; if parsed-span-start is a date, then use it, otherwise use nil
        (assoc :span_start (if (date? parsed-span-start) parsed-span-start nil))
        ; if parsed-span-end is a date, then use it, otherwise use nil
        (assoc :span_end (if (date? parsed-span-end) parsed-span-end nil)))))
        
(defn create-title! [params]
  (let [missing (filter #(nil? (params %)) [:newspaper_table_id :author_id])
        optional-keys [:span_start :span_end :publication_title
                       :attributed_author_name :common_title :author_of
                       :additional_info :inscribed_author_nationality
                       :inscribed_author_gender :information_source
                       :length :trove_source :also_published :name_category
                       :curated_dataset :added_by]]
    (if (empty? missing)
      (jdbc/with-transaction [t-conn db/*db*]
        (try
          (->> params
               (parse-span-dates)
               (nil-fill-default-params optional-keys)
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



(defn update-title!
  "Update the values of a title by its primary key (id)."
  [id new-params]
  {:pre [(number? id) (map? new-params)]}
  (jdbc/with-transaction [t-conn db/*db*]
    (let [existing-title (get-title id)
          clean-params (-> new-params (parse-span-dates) (drop-nil-params) (drop-blank-params))
          title-for-update (-> existing-title
                               (merge clean-params)
                               (select-keys updateable-title-keys)
                               (assoc :id id)
                               (parse-span-dates))]
      (println "existing-title: " existing-title)
      (println "title-for-update: " title-for-update)
      (cond (empty? existing-title)
            (throw (ex-info "No title found with that ID!"
                            {:cde/error-id ::no-title-found
                             :error "No title found with ID!"}))
            (empty? clean-params)
            (throw (ex-info "No valid parameters provided for update!"
                            {:cde/error-id ::no-valid-params
                             :error "No valid parameters provided for update!"}))
            :else
            (try (db/update-title!* t-conn title-for-update)
                 (catch Exception e
                   (throw (ex-info "Error updating title"
                                   {:cde/error-id ::update-title-exception
                                    :error (.getMessage e)}))))))))


(defn get-titles
  "Get an unfiltered list of titles from the db.
   
   Accepts optional limit & offset params (defaulting to 50 & 0 respectively).
   Limit is capped at 500 for performance reasons.

   Returns a map containing a list of titles, along with next/previous links."
  ([]
   (get-titles 50 0))
  ([limit]
   (get-titles limit 0))
  ([limit offset]
   (let [limit (min limit 500)
         titles (db/get-titles* {:limit limit :offset offset})
         next (if (= limit (count titles))
                (str "/titles?limit=" limit "&offset=" (+ offset limit))
                nil)
         prev (if (> offset 0)
                (str "/titles?limit=" limit "&offset=" (max (- offset limit) 0))
                nil)]
     {:results titles
      :next next
      :previous prev})))