(ns cde.db.user
  (:require
   [next.jdbc :as jdbc]
   [cde.db.core :as db]))

;; (defn create-user! [email]
;;   (jdbc/with-transaction [t-conn db/*db*]
;;       (if-not (empty? (db/get-user-for-auth-by-email* t-conn {:email email}))
;;         (throw (ex-info "A user already exists with this email!"
;;                         {:cde/error-id ::duplicate-user-email
;;                          :error "User already exists with this email!"}))
;;         (db/create-user!* t-conn
;;                           {:email email}))))

;; (defn get-user-id!
;;   [email]
;;   (let [(db/get-user-for-auth-by-email* {:email email})]
;;   ))