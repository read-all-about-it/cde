(ns cde.db.newspaper-test
  "Tests for newspaper entity CRUD operations.

  Tests cover:
  - Newspaper creation with required and optional fields
  - Newspaper retrieval by ID and Trove ID
  - Newspaper updates
  - Duplicate detection by trove_newspaper_id
  - Paginated listing
  - Error handling for missing/invalid data"
  (:require
   [cde.db.newspaper :as newspaper]
   [cde.test-fixtures :refer [with-db with-rollback db-available?]]
   [clojure.test :refer :all]))

(use-fixtures :once with-db)
(use-fixtures :each with-rollback)

(defn- unique-trove-id
  "Generates a unique Trove newspaper ID for testing.
   Uses a large random range to avoid collisions with existing data."
  []
  (+ 9000000 (rand-int 1000000)))

(deftest test-create-newspaper
  (testing "Creating a newspaper with required fields succeeds"
    (if (db-available?)
      (let [newspaper-id (newspaper/create-newspaper!
                          {:title "The Test Gazette"
                           :trove_newspaper_id (unique-trove-id)})]
        (is (number? newspaper-id))
        (is (pos? newspaper-id)))
      (is true "Skipping - no database configured"))))

(deftest test-create-newspaper-with-optional-fields
  (testing "Creating a newspaper with all fields succeeds"
    (if (db-available?)
      (let [newspaper-id (newspaper/create-newspaper!
                          {:title "The Sydney Morning Herald"
                           :trove_newspaper_id (unique-trove-id)
                           :common_title "SMH"
                           :location "Sydney, NSW"
                           :colony_state "New South Wales"
                           :newspaper_type "Daily"
                           :start_year 1831
                           :end_year 2023
                           :details "Major Australian newspaper"})]
        (is (number? newspaper-id))
        (let [retrieved (newspaper/get-newspaper newspaper-id)]
          (is (= "The Sydney Morning Herald" (:title retrieved)))
          (is (= "SMH" (:common_title retrieved)))
          (is (= "Sydney, NSW" (:location retrieved)))
          (is (= "New South Wales" (:colony_state retrieved)))))
      (is true "Skipping - no database configured"))))

(deftest test-create-newspaper-missing-required-fields
  (testing "Creating a newspaper without title throws exception"
    (if (db-available?)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing required parameters"
           (newspaper/create-newspaper! {:trove_newspaper_id (unique-trove-id)})))
      (is true "Skipping - no database configured")))

  (testing "Creating a newspaper without trove_newspaper_id throws exception"
    (if (db-available?)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing required parameters"
           (newspaper/create-newspaper! {:title "Some Paper"})))
      (is true "Skipping - no database configured"))))

(deftest test-create-newspaper-duplicate-trove-id
  (testing "Creating a newspaper with duplicate trove_newspaper_id throws exception"
    (if (db-available?)
      (let [trove-id (unique-trove-id)]
        ;; Create first newspaper
        (newspaper/create-newspaper!
         {:title "First Paper"
          :trove_newspaper_id trove-id})
        ;; Try to create duplicate
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"already exists"
             (newspaper/create-newspaper!
              {:title "Second Paper"
               :trove_newspaper_id trove-id}))))
      (is true "Skipping - no database configured"))))

(deftest test-get-newspaper
  (testing "Getting a newspaper by ID returns the newspaper"
    (if (db-available?)
      (let [trove-id (unique-trove-id)
            newspaper-id (newspaper/create-newspaper!
                          {:title "Retrieval Test Paper"
                           :trove_newspaper_id trove-id})
            retrieved (newspaper/get-newspaper newspaper-id)]
        (is (map? retrieved))
        (is (= "Retrieval Test Paper" (:title retrieved)))
        (is (= newspaper-id (:id retrieved))))
      (is true "Skipping - no database configured"))))

(deftest test-get-newspaper-not-found
  (testing "Getting a non-existent newspaper throws exception"
    (if (db-available?)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"No newspaper found"
           (newspaper/get-newspaper 999999)))
      (is true "Skipping - no database configured"))))

(deftest test-get-newspaper-by-trove-id
  (testing "Getting a newspaper by Trove ID returns the newspaper"
    (if (db-available?)
      (let [trove-id (unique-trove-id)
            newspaper-id (newspaper/create-newspaper!
                          {:title "Trove ID Test Paper"
                           :trove_newspaper_id trove-id})
            retrieved (newspaper/get-newspaper-by-trove-id trove-id)]
        (is (seq retrieved))
        (is (= "Trove ID Test Paper" (:title (first retrieved)))))
      (is true "Skipping - no database configured"))))

(deftest test-update-newspaper
  (testing "Updating a newspaper modifies the record"
    (if (db-available?)
      (let [trove-id (unique-trove-id)
            newspaper-id (newspaper/create-newspaper!
                          {:title "Original Title"
                           :trove_newspaper_id trove-id
                           :location "Melbourne"})
            _ (newspaper/update-newspaper! newspaper-id
                                           {:title "Updated Title"
                                            :colony_state "Victoria"})
            updated (newspaper/get-newspaper newspaper-id)]
        (is (= "Updated Title" (:title updated)))
        (is (= "Victoria" (:colony_state updated)))
        ;; Unchanged field should remain
        (is (= "Melbourne" (:location updated))))
      (is true "Skipping - no database configured"))))

(deftest test-trove-newspaper-id->newspaper-id
  (testing "Converting Trove ID to database ID works"
    (if (db-available?)
      (let [trove-id (unique-trove-id)
            newspaper-id (newspaper/create-newspaper!
                          {:title "Conversion Test Paper"
                           :trove_newspaper_id trove-id})
            converted-id (newspaper/trove-newspaper-id->newspaper-id trove-id)]
        (is (= newspaper-id converted-id)))
      (is true "Skipping - no database configured")))

  (testing "Converting non-existent Trove ID returns nil"
    (if (db-available?)
      (let [result (newspaper/trove-newspaper-id->newspaper-id 888888888)]
        (is (nil? result)))
      (is true "Skipping - no database configured"))))

(deftest test-get-newspapers-pagination
  (testing "get-newspapers returns paginated results with next/prev links"
    (if (db-available?)
      (do
        ;; Create a few newspapers
        (dotimes [i 5]
          (newspaper/create-newspaper!
           {:title (str "Pagination Test Paper " i)
            :trove_newspaper_id (unique-trove-id)}))
        (let [result (newspaper/get-newspapers 2 0)]
          (is (map? result))
          (is (contains? result :results))
          (is (contains? result :next))
          (is (contains? result :previous))
          (is (<= (count (:results result)) 2))))
      (is true "Skipping - no database configured"))))
