(ns cde.db.author
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [nil-fill-default-params drop-nil-params]]))

(def ^:private updateable-author-keys
  [:common_name
   :other_name
   :gender
   :nationality
   :nationality_details
   :author_details])


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


(defn update-author!
  "Update the values of an existing author by ID."
  [id new-params]
  {:pre [(number? id) (map? new-params)]}
  (jdbc/with-transaction [t-conn db/*db*]
    (let [existing-author (get-author id)
          clean-params (drop-nil-params new-params)
          author-for-update (-> existing-author
                                (merge clean-params)
                                (select-keys updateable-author-keys)
                                (assoc :id id))]
      (cond (empty? existing-author)
            (throw (ex-info "No author found with that ID!"
                            {:cde/error-id ::no-author-found
                             :error "No author found with that ID!"}))
            :else (try
                    (db/update-author!* t-conn author-for-update)
                    (catch Exception e
                      (throw (ex-info "Error updating author"
                                      {:cde/error-id ::update-author-exception
                                       :error (.getMessage e)}))))))))


(defn get-authors
  "Get an unfiltered list of authors from the db.
   
   Accepts optional limit & offset params (defaulting to 50 & 0 respectively).
   Limit is capped at 500 for performance reasons.

   Returns a map containing a list of authors, along with next/previous links."
  ([]
   (get-authors 50 0))
  ([limit]
   (get-authors limit 0))
  ([limit offset]
   (let [limit (min limit 500)
         authors (db/get-authors* {:limit limit :offset offset})
         next (if (= limit (count authors))
                (str "/authors?limit=" limit "&offset=" (+ offset limit))
                nil)
         prev (if (> offset 0)
                (str "/authors?limit=" limit "&offset=" (max (- offset limit) 0))
                nil)]
     {:results authors
      :next next
      :previous prev})))