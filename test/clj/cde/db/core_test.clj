(ns cde.db.core-test
  (:require
   [cde.db.core :refer [*db*] :as db]
   [cde.db.user :as user]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [cde.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'cde.config/env
     #'cde.db.core/*db*)
    ;; Only run migrations if database URL is configured
    (when-let [db-url (:database-url env)]
      (try
        (migrations/migrate ["migrate"] {:database-url db-url})
        (catch Exception e
          (println "Warning: Could not run migrations:" (.getMessage e)))))
    (f)))

(deftest test-users
  (testing "User creation and retrieval"
    (if (and *db* (:database-url env))
      (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
        (let [test-email "test@example.com"
              user-record (user/get-or-create-user! test-email)]
          (is (map? user-record))
          (is (= test-email (:email user-record)))
          (is (number? (:id user-record)))))
      (is true "Skipping database test - no database configured"))))
