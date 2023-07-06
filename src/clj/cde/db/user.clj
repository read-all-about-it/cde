(ns cde.db.user
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn get-or-create-user!
  "Given an email address, create a new user in the database.
   If one already exists, return that user record."
  [email]
  {:pre [(string? email)]}
  (jdbc/with-transaction [t-conn db/*db*]
    (let [user (db/get-user-from-email* t-conn {:email email})]
      (println "user: " user)
      (if (empty? user)
        (try
          (let [new-user (db/create-user!* t-conn {:email email})]
            (println "new-user: " new-user)
            {:id (:id new-user) :email email})
          (catch Exception e
            (throw (ex-info "Error creating user!"
                            {:cde/error-id ::error-creating-user
                             :error (str "Error creating user!" (.getMessage e))}))))
        user))))