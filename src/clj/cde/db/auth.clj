(ns cde.db.auth
  (:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

(defn create-user! [username email password]
  (jdbc/with-transaction [t-conn db/*db*]
    (if-not (empty? (db/get-user-for-auth-by-username* t-conn {:username username}))
      (throw (ex-info "A user already exists with this username!"
                      {:cde/error-id ::duplicate-user-username
                       :error "User already exists with this username!"}))
      (if-not (empty? (db/get-user-for-auth-by-email* t-conn {:email email}))
        (throw (ex-info "A user already exists with this email!"
                        {:cde/error-id ::duplicate-user-email
                         :error "User already exists with this email!"}))
        (db/create-user!* t-conn
                          {:username username
                           :email email
                           :password (hashers/derive password)})))))

(defn authenticate-user [email password]
  (let [{hashed :password :as user} (db/get-user-for-auth-by-email* {:email email})]
    (when (hashers/check password hashed)
      (dissoc user :password))))

(defn get-user-profile [user-id]
  (let [profile (jdbc/with-transaction [t-conn db/*db*] 
                  (db/get-user-profile* t-conn {:id user-id}))]
    (if (empty? profile)
      (throw (ex-info "No profile found for a user with that ID!"
                      {:cde/error-id ::no-user-found
                       :error "No profile found for a user with ID!"}))
      (dissoc profile :id)
      )))