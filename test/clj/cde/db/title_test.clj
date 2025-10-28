(ns cde.db.title-test
  "Tests for title entity CRUD operations.

  Tests cover:
  - Title creation with required and optional fields
  - Title retrieval by ID (with and without joins)
  - Title updates
  - Paginated listing
  - Error handling for missing/invalid data

  Note: Title tests require author and newspaper records to exist
  (foreign key constraints), so we create these as part of test setup."
  (:require
   [cde.db.title :as title]
   [cde.db.author :as author]
   [cde.db.newspaper :as newspaper]
   [cde.test-fixtures :refer [with-db with-rollback db-available?]]
   [clojure.test :refer :all]))

(use-fixtures :once with-db)
(use-fixtures :each with-rollback)

(defn- create-test-author!
  "Helper to create a test author and return its ID."
  []
  (author/create-author! {:common_name "Test Author for Title"}))

(defn- create-test-newspaper!
  "Helper to create a test newspaper and return its ID.
   Uses a random Trove ID to avoid conflicts."
  []
  (newspaper/create-newspaper!
   {:title "Test Newspaper for Title"
    :trove_newspaper_id (+ 80000 (rand-int 10000))}))

(deftest test-create-title
  (testing "Creating a title with required fields succeeds"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            title-id (title/create-title!
                      {:author_id author-id
                       :newspaper_table_id newspaper-id})]
        (is (number? title-id))
        (is (pos? title-id)))
      (is true "Skipping - no database configured"))))

(deftest test-create-title-with-optional-fields
  (testing "Creating a title with all fields succeeds"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            title-id (title/create-title!
                      {:author_id author-id
                       :newspaper_table_id newspaper-id
                       :publication_title "The Adventures of Test Hero"
                       :common_title "Test Hero"
                       :attributed_author_name "T. Author"
                       :span_start "1890-01-15"
                       :span_end "1890-06-30"
                       :length 0
                       :additional_info "A thrilling test adventure"})]
        (is (number? title-id))
        (let [retrieved (title/get-title title-id)]
          (is (= "The Adventures of Test Hero" (:publication_title retrieved)))
          (is (= "Test Hero" (:common_title retrieved)))
          (is (= 0 (:length retrieved)))))
      (is true "Skipping - no database configured"))))

(deftest test-create-title-missing-required-fields
  (testing "Creating a title without author_id throws exception"
    (if (db-available?)
      (let [newspaper-id (create-test-newspaper!)]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Missing required parameters"
             (title/create-title! {:newspaper_table_id newspaper-id}))))
      (is true "Skipping - no database configured")))

  (testing "Creating a title without newspaper_table_id throws exception"
    (if (db-available?)
      (let [author-id (create-test-author!)]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Missing required parameters"
             (title/create-title! {:author_id author-id}))))
      (is true "Skipping - no database configured"))))

(deftest test-get-title
  (testing "Getting a title by ID returns the title"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            title-id (title/create-title!
                      {:author_id author-id
                       :newspaper_table_id newspaper-id
                       :publication_title "Retrieval Test Title"})
            retrieved (title/get-title title-id)]
        (is (map? retrieved))
        (is (= "Retrieval Test Title" (:publication_title retrieved)))
        (is (= title-id (:id retrieved))))
      (is true "Skipping - no database configured"))))

(deftest test-get-title-with-join
  (testing "Getting a title with join includes author and newspaper names"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            title-id (title/create-title!
                      {:author_id author-id
                       :newspaper_table_id newspaper-id
                       :publication_title "Join Test Title"})
            retrieved (title/get-title title-id true)]
        (is (map? retrieved))
        (is (contains? retrieved :author_common_name))
        (is (contains? retrieved :newspaper_title)))
      (is true "Skipping - no database configured"))))

(deftest test-get-title-not-found
  (testing "Getting a non-existent title throws exception"
    (if (db-available?)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"No title found"
           (title/get-title 999999)))
      (is true "Skipping - no database configured"))))

(deftest test-update-title
  (testing "Updating a title modifies the record"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            title-id (title/create-title!
                      {:author_id author-id
                       :newspaper_table_id newspaper-id
                       :publication_title "Original Title"
                       :common_title "Original"})
            _ (title/update-title! title-id
                                   {:publication_title "Updated Title"
                                    :length 1})
            updated (title/get-title title-id)]
        (is (= "Updated Title" (:publication_title updated)))
        (is (= 1 (:length updated)))
        ;; Unchanged field should remain
        (is (= "Original" (:common_title updated))))
      (is true "Skipping - no database configured"))))

(deftest test-get-titles-pagination
  (testing "get-titles returns paginated results with next/prev links"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)]
        ;; Create a few titles
        (dotimes [i 5]
          (title/create-title!
           {:author_id author-id
            :newspaper_table_id newspaper-id
            :publication_title (str "Pagination Test Title " i)}))
        (let [result (title/get-titles 2 0)]
          (is (map? result))
          (is (contains? result :results))
          (is (contains? result :next))
          (is (contains? result :previous))
          (is (<= (count (:results result)) 2))))
      (is true "Skipping - no database configured"))))
