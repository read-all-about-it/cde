(ns cde.title
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]
   [cde.utils :refer [kebab->snake nil-fill-default-params]]))


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
               (nil-fill-default-params optional-keys)
               (kebab->snake)
               (db/create-title!* t-conn)
               (first)
               (:id)) ;; get id of the inserted title (if successful)
          (catch Exception e
            (throw (ex-info "Error creating title"
                            {:cde/error-id ::create-title-exception
                             :error (.getMessage e)})))))
      (throw (ex-info (apply str "Missing required parameters: " (interpose " " missing))
                      {:cde/error-id ::missing-required-params
                       :error (apply str "Missing required parameters: " (interpose " " missing))
                       :missing missing})))))