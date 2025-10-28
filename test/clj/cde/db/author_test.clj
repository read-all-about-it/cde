(ns cde.db.author-test
  "Tests for author entity CRUD operations.

  Tests cover:
  - Author creation with required and optional fields
  - Author retrieval by ID
  - Author updates
  - Paginated listing
  - Error handling for missing/invalid data"
  (:require
   [cde.db.author :as author]
   [cde.test-fixtures :refer [with-db with-rollback db-available?]]
   [clojure.test :refer :all]))

(use-fixtures :once with-db)
(use-fixtures :each with-rollback)

(deftest test-create-author
  (testing "Creating an author with required fields succeeds"
    (if (db-available?)
      (let [author-id (author/create-author! {:common_name "Test Author"})]
        (is (number? author-id))
        (is (pos? author-id)))
      (is true "Skipping - no database configured"))))

(deftest test-create-author-with-optional-fields
  (testing "Creating an author with all fields succeeds"
    (if (db-available?)
      (let [author-id (author/create-author!
                       {:common_name "Jane Doe"
                        :other_name "J.D., Jane Smith"
                        :gender "Female"
                        :nationality "Australian"
                        :nationality_details "Born in Sydney"
                        :author_details "Prolific writer of serialised fiction"})]
        (is (number? author-id))
        (let [retrieved (author/get-author author-id)]
          (is (= "Jane Doe" (:common_name retrieved)))
          (is (= "J.D., Jane Smith" (:other_name retrieved)))
          (is (= "Female" (:gender retrieved)))
          (is (= "Australian" (:nationality retrieved)))))
      (is true "Skipping - no database configured"))))

(deftest test-create-author-missing-required-field
  (testing "Creating an author without common_name throws exception"
    (if (db-available?)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Missing required parameter"
           (author/create-author! {:other_name "Some Name"})))
      (is true "Skipping - no database configured"))))

(deftest test-get-author
  (testing "Getting an author by ID returns the author"
    (if (db-available?)
      (let [author-id (author/create-author! {:common_name "Retrieval Test"})
            retrieved (author/get-author author-id)]
        (is (map? retrieved))
        (is (= "Retrieval Test" (:common_name retrieved)))
        (is (= author-id (:id retrieved))))
      (is true "Skipping - no database configured"))))

(deftest test-get-author-not-found
  (testing "Getting a non-existent author throws exception"
    (if (db-available?)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"No author found"
           (author/get-author 999999)))
      (is true "Skipping - no database configured"))))

(deftest test-update-author
  (testing "Updating an author modifies the record"
    (if (db-available?)
      (let [author-id (author/create-author! {:common_name "Original Name"
                                              :nationality "British"})
            _ (author/update-author! author-id {:common_name "Updated Name"
                                                :gender "Male"})
            updated (author/get-author author-id)]
        (is (= "Updated Name" (:common_name updated)))
        (is (= "Male" (:gender updated)))
        ;; Unchanged field should remain
        (is (= "British" (:nationality updated))))
      (is true "Skipping - no database configured"))))

(deftest test-get-authors-pagination
  (testing "get-authors returns paginated results with next/prev links"
    (if (db-available?)
      (do
        ;; Create a few authors
        (dotimes [i 5]
          (author/create-author! {:common_name (str "Pagination Test Author " i)}))
        (let [result (author/get-authors 2 0)]
          (is (map? result))
          (is (contains? result :results))
          (is (contains? result :next))
          (is (contains? result :previous))
          (is (<= (count (:results result)) 2))))
      (is true "Skipping - no database configured"))))

(deftest test-get-authors-limit-cap
  (testing "get-authors caps limit at 500"
    (if (db-available?)
      ;; Just verify it doesn't throw with a large limit
      (let [result (author/get-authors 1000 0)]
        (is (map? result))
        (is (contains? result :results)))
      (is true "Skipping - no database configured"))))
