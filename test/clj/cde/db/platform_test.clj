(ns cde.db.platform-test
  "Tests for platform-level statistics and metrics.

   Tests cover aggregate count queries for newspapers, authors,
   titles, and chapters."
  (:require
   [cde.db.platform :as platform]
   [cde.db.author :as author]
   [cde.db.newspaper :as newspaper]
   [cde.db.title :as title]
   [cde.db.chapter :as chapter]
   [cde.trove :as trove]
   [cde.test-fixtures :refer [with-db with-rollback db-available?]]
   [clojure.test :refer :all]))

(use-fixtures :once with-db)
(use-fixtures :each with-rollback)

;; Test helpers

(defn- create-test-author!
  "Creates a test author and returns its ID."
  []
  (author/create-author! {:common_name "Platform Test Author"}))

(defn- create-test-newspaper!
  "Creates a test newspaper with a random Trove ID."
  []
  (newspaper/create-newspaper!
   {:title "Platform Test Newspaper"
    :trove_newspaper_id (+ 96000 (rand-int 4000))}))

(defn- create-test-title!
  "Creates a test title with required foreign keys."
  [author-id newspaper-id]
  (title/create-title!
   {:publication_title "Platform Test Title"
    :author_id author-id
    :newspaper_table_id newspaper-id}))

(def ^:private mock-trove-article
  "Mock response from Trove API for testing chapter creation."
  {:chapter_html "<p>Platform test chapter content</p>"
   :chapter_text "Platform test chapter content"
   :article_url "https://trove.nla.gov.au/newspaper/article/99999"
   :corrections 0
   :word_count 25
   :illustrated false
   :pub_day 1
   :pub_month 1
   :pub_year 1900
   :trove_api_status 200})

;; Tests

(deftest test-get-platform-statistics-returns-expected-structure
  (testing "get-platform-statistics returns all required count keys"
    (if (db-available?)
      (let [stats (platform/get-platform-statistics)]
        (is (map? stats))
        (is (contains? stats :newspaper-count))
        (is (contains? stats :author-count))
        (is (contains? stats :title-count))
        (is (contains? stats :chapter-count)))
      (is true "Skipping - no database configured"))))

(deftest test-get-platform-statistics-returns-integers
  (testing "All counts are non-negative integers"
    (if (db-available?)
      (let [stats (platform/get-platform-statistics)]
        (is (integer? (:newspaper-count stats)))
        (is (integer? (:author-count stats)))
        (is (integer? (:title-count stats)))
        (is (integer? (:chapter-count stats)))
        (is (>= (:newspaper-count stats) 0))
        (is (>= (:author-count stats) 0))
        (is (>= (:title-count stats) 0))
        (is (>= (:chapter-count stats) 0)))
      (is true "Skipping - no database configured"))))

(deftest test-get-platform-statistics-reflects-new-records
  (testing "Creating records increases the respective counts"
    (if (db-available?)
      (let [initial-stats (platform/get-platform-statistics)
            initial-author-count (:author-count initial-stats)
            initial-newspaper-count (:newspaper-count initial-stats)
            ;; Create new records
            _ (create-test-author!)
            _ (create-test-newspaper!)
            updated-stats (platform/get-platform-statistics)]
        (is (= (inc initial-author-count) (:author-count updated-stats)))
        (is (= (inc initial-newspaper-count) (:newspaper-count updated-stats))))
      (is true "Skipping - no database configured"))))

(deftest test-get-platform-statistics-counts-titles
  (testing "Creating a title increases the title count"
    (if (db-available?)
      (let [initial-stats (platform/get-platform-statistics)
            initial-title-count (:title-count initial-stats)
            author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            _ (create-test-title! author-id newspaper-id)
            updated-stats (platform/get-platform-statistics)]
        (is (= (inc initial-title-count) (:title-count updated-stats))))
      (is true "Skipping - no database configured"))))

(deftest test-get-platform-statistics-counts-chapters
  (testing "Creating a chapter increases the chapter count"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [initial-stats (platform/get-platform-statistics)
              initial-chapter-count (:chapter-count initial-stats)
              author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              _ (chapter/create-chapter!
                 {:title_id title-id
                  :trove_article_id (+ 900000 (rand-int 100000))})
              updated-stats (platform/get-platform-statistics)]
          (is (= (inc initial-chapter-count) (:chapter-count updated-stats)))))
      (is true "Skipping - no database configured"))))

(deftest test-get-platform-statistics-multiple-records
  (testing "Creating multiple records of each type updates counts correctly"
    (if (db-available?)
      (let [initial-stats (platform/get-platform-statistics)
            ;; Create 3 authors
            _ (dotimes [_ 3] (create-test-author!))
            ;; Create 2 newspapers
            _ (dotimes [_ 2] (create-test-newspaper!))
            updated-stats (platform/get-platform-statistics)]
        (is (= (+ 3 (:author-count initial-stats)) (:author-count updated-stats)))
        (is (= (+ 2 (:newspaper-count initial-stats)) (:newspaper-count updated-stats))))
      (is true "Skipping - no database configured"))))
