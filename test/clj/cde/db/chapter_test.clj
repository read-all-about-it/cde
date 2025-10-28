(ns cde.db.chapter-test
  "Tests for chapter entity CRUD operations.

   Chapter tests require creating supporting entities (author, newspaper, title)
   due to foreign key constraints. Tests mock Trove API calls to avoid external
   dependencies."
  (:require
   [cde.db.chapter :as chapter]
   [cde.db.author :as author]
   [cde.db.newspaper :as newspaper]
   [cde.db.title :as title]
   [cde.trove :as trove]
   [cde.test-fixtures :refer [with-db with-rollback db-available?]]
   [clojure.test :refer :all]))

(use-fixtures :once with-db)
(use-fixtures :each with-rollback)

;; Test helpers

(defn- create-test-author!
  "Creates a test author and returns its ID."
  []
  (author/create-author! {:common_name "Test Author for Chapter"}))

(defn- create-test-newspaper!
  "Creates a test newspaper with a random Trove ID to avoid conflicts."
  []
  (newspaper/create-newspaper!
   {:title "Test Newspaper for Chapter"
    :trove_newspaper_id (+ 90000 (rand-int 10000))}))

(defn- create-test-title!
  "Creates a test title with required foreign keys."
  [author-id newspaper-id]
  (title/create-title!
   {:publication_title "Test Title for Chapter"
    :author_id author-id
    :newspaper_table_id newspaper-id}))

(def ^:private mock-trove-article
  "Mock response from Trove API for testing."
  {:chapter_html "<p>Test chapter content</p>"
   :chapter_text "Test chapter content"
   :article_url "https://trove.nla.gov.au/newspaper/article/12345"
   :corrections 0
   :word_count 100
   :illustrated false
   :pub_day 15
   :pub_month 6
   :pub_year 1890
   :trove_api_status 200})

;; Tests

(deftest test-create-chapter-with-mocked-trove
  (testing "Creating a chapter with mocked Trove API succeeds"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              trove-article-id (+ 100000 (rand-int 100000))
              chapter-id (chapter/create-chapter!
                          {:title_id title-id
                           :trove_article_id trove-article-id})]
          (is (number? chapter-id))
          (is (pos? chapter-id))))
      (is true "Skipping - no database configured"))))

(deftest test-create-chapter-missing-required-params
  (testing "Creating a chapter without required params throws exception"
    (if (db-available?)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Missing required parameters"
                            (chapter/create-chapter! {:chapter_number 1})))
      (is true "Skipping - no database configured"))))

(deftest test-create-chapter-invalid-title-id
  (testing "Creating a chapter with non-existent title throws exception"
    (if (db-available?)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No title found"
                            (chapter/create-chapter!
                             {:title_id 999999
                              :trove_article_id 12345})))
      (is true "Skipping - no database configured"))))

(deftest test-get-chapter
  (testing "Getting a chapter by ID returns expected fields"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              trove-article-id (+ 200000 (rand-int 100000))
              chapter-id (chapter/create-chapter!
                          {:title_id title-id
                           :trove_article_id trove-article-id})
              fetched (chapter/get-chapter chapter-id)]
          (is (= chapter-id (:id fetched)))
          (is (= title-id (:title_id fetched)))
          (is (= trove-article-id (:trove_article_id fetched)))
          ;; Verify excluded fields are not present
          (is (nil? (:chapter_text_vector fetched)))
          (is (nil? (:export_title fetched)))))
      (is true "Skipping - no database configured"))))

(deftest test-get-chapter-not-found
  (testing "Getting a non-existent chapter throws exception"
    (if (db-available?)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No chapter found"
                            (chapter/get-chapter 999999)))
      (is true "Skipping - no database configured"))))

(deftest test-get-chapters-in-title
  (testing "Getting chapters for a title returns all chapters"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              _ (chapter/create-chapter!
                 {:title_id title-id
                  :trove_article_id (+ 300000 (rand-int 100000))
                  :chapter_number 1})
              _ (chapter/create-chapter!
                 {:title_id title-id
                  :trove_article_id (+ 400000 (rand-int 100000))
                  :chapter_number 2})
              chapters (chapter/get-chapters-in-title title-id)]
          (is (= 2 (count chapters)))))
      (is true "Skipping - no database configured"))))

(deftest test-trove-article-id->chapter-id
  (testing "Looking up chapter by Trove article ID returns correct ID"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              trove-article-id (+ 500000 (rand-int 100000))
              chapter-id (chapter/create-chapter!
                          {:title_id title-id
                           :trove_article_id trove-article-id})
              found-id (chapter/trove-article-id->chapter-id trove-article-id)]
          (is (= chapter-id found-id))))
      (is true "Skipping - no database configured"))))

(deftest test-trove-article-id->chapter-id-not-found
  (testing "Looking up non-existent Trove article ID returns nil"
    (if (db-available?)
      (let [result (chapter/trove-article-id->chapter-id 999999999)]
        (is (nil? result)))
      (is true "Skipping - no database configured"))))

(deftest test-update-chapter
  (testing "Updating a chapter modifies the specified fields"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              trove-article-id (+ 600000 (rand-int 100000))
              chapter-id (chapter/create-chapter!
                          {:title_id title-id
                           :trove_article_id trove-article-id
                           :chapter_number 1})
              _ (chapter/update-chapter! chapter-id {:chapter_number "5"
                                                     :chapter_title "Updated Title"})
              updated (chapter/get-chapter chapter-id)]
          ;; chapter_number is stored as text in the database
          (is (= "5" (:chapter_number updated)))
          (is (= "Updated Title" (:chapter_title updated)))))
      (is true "Skipping - no database configured"))))

(deftest test-update-chapter-not-found
  (testing "Updating a non-existent chapter throws exception"
    (if (db-available?)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No chapter found"
                            (chapter/update-chapter! 999999 {:chapter_number 1})))
      (is true "Skipping - no database configured"))))

(deftest test-get-chapters-pagination
  (testing "get-chapters returns paginated results with next/prev links"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              ;; Create 3 chapters
              _ (dotimes [n 3]
                  (chapter/create-chapter!
                   {:title_id title-id
                    :trove_article_id (+ 700000 n (rand-int 1000))
                    :chapter_number (inc n)}))
              result (chapter/get-chapters 2 0)]
          (is (map? result))
          (is (contains? result :results))
          (is (contains? result :next))
          (is (contains? result :previous))
          ;; Verify chapter_text is excluded from results
          (is (every? #(nil? (:chapter_text %)) (:results result)))))
      (is true "Skipping - no database configured"))))

(deftest test-get-chapters-default-params
  (testing "get-chapters works with default parameters"
    (if (db-available?)
      (let [result (chapter/get-chapters)]
        (is (map? result))
        (is (contains? result :results)))
      (is true "Skipping - no database configured"))))
