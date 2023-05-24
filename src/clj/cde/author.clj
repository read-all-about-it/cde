(ns cde.author
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]))


(defn create-author! [params]
  (let [missing (filter #(nil? (params %)) [:common-name])
        optional-keys [:other-name :gender :nationality
                       :nationality-details :author-details :added-by]]
    (if (empty? missing)
      (jdbc/with-transaction [t-conn db/*db*]
        (try
          (->> params
               (nil-fill-default-params optional-keys)
               (kebab->snake)
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
      (throw (ex-info "No nationalities found!"
                      {:cde/error-id ::no-nationalities-found
                       :error "No nationalities found!"}))
      (into [] (map :nationality nationalities)))))