(ns cde.db.user
  "User entity operations.

   Provides functions for user management. Users are created automatically
   when they first authenticate via Auth0, using their email address as
   the unique identifier."
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn get-or-create-user!
  "Gets an existing user by email or creates a new one if not found.

   Used during authentication to ensure a user record exists for the
   authenticated email address. Returns the user record with :id and :email."
  [email]
  {:pre [(string? email)]}
  (jdbc/with-transaction [t-conn db/*db*]
    (let [user (db/get-user-from-email* t-conn {:email email})]
      (if (empty? user)
        (try
          (let [new-user (db/create-user!* t-conn {:email email})]
            {:id (:id new-user) :email email})
          (catch Exception e
            (throw (ex-info "Error creating user!"
                            {:cde/error-id ::error-creating-user
                             :error (str "Error creating user!" (.getMessage e))}))))
        user))))
