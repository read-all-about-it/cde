(ns cde.db.author
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [nil-fill-default-params]]))


(defn create-author! [params]
  (let [missing (filter #(nil? (params %)) [:common_name])
        optional-keys [:other_name :gender :nationality
                       :nationality_details :author_details :added_by]]
    (if (empty? missing)
      (jdbc/with-transaction [t-conn db/*db*]
        (try
          (->> params
               (nil-fill-default-params optional-keys)
               (db/create-author!* t-conn)
               (:id)) ;; get id of the inserted author (if successful)
          (catch Exception e
            (throw (ex-info "Error creating author"
                            {:cde/error-id ::create-author-exception
                             :error (.getMessage e)})))))
      (throw (ex-info "Missing required parameter: common-name"
                      {:cde/error-id ::missing-required-params
                       :error "Missing required parameter: common-name"
                       :missing missing})))))

(defn get-author [id]
  (let [author (db/get-author-by-id* {:id id})]
    (if (empty? author)
      (throw (ex-info "No author found with that ID!"
                      {:cde/error-id ::no-author-found
                       :error "No author found with ID!"}))
      author)))

(defn get-nationalities []
  (let [nationalities (db/get-unique-author-nationalities*)]
    (if (empty? nationalities)
      (throw (ex-info "No author nationalities found!"
                      {:cde/error-id ::no-nationalities-found
                       :error "No author nationalities found!"}))
      (into [] (map :nationality nationalities)))))

(defn get-genders []
  (let [genders (db/get-unique-author-genders*)]
    (if (empty? genders)
      (throw (ex-info "No author genders found!"
                      {:cde/error-id ::no-genders-found
                       :error "No author genders found!"}))
      (into [] (map :gender genders)))))

(defn get-titles-by-author [author-id]
  (let [titles (db/get-all-titles-by-author-id* {:author_id author-id})]
    (if (empty? titles)
      (throw (ex-info "No titles found by that author!"
                      {:cde/error-id ::no-titles-found
                       :error "No titles found by that author!"}))
      titles)))


(defn get-terse-author-list
  "Get a 'terse' list of all authors, ordered by common name.
   Only return: id, common_name, other_name."
  []
  (let [authors (db/get-terse-author-list* {})]
    (if (empty? authors)
      (throw (ex-info "No authors found!"
                      {:cde/error-id ::no-authors-found
                       :error "No authors found!"}))
      authors)))