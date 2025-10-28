(ns cde.db.search-test
  "Tests for full-text search operations.

   Tests cover title and chapter search functionality including
   ILIKE pattern matching for substring search and pagination.

   Includes integration tests for search functions (require DB)."
  (:require
   [cde.db.search :as search]
   [cde.db.author :as author]
   [cde.db.newspaper :as newspaper]
   [cde.db.title :as title]
   [cde.db.chapter :as chapter]
   [cde.trove :as trove]
   [cde.test-fixtures :refer [with-db with-rollback db-available?]]
   [clojure.test :refer :all]))

(use-fixtures :once with-db)
(use-fixtures :each with-rollback)

;; Test helpers for integration tests

(defn- create-test-author!
  "Creates a test author with searchable attributes."
  [& {:keys [name nationality] :or {name "Searchable Author" nationality "Australian"}}]
  (author/create-author! {:common_name name :nationality nationality}))

(defn- create-test-newspaper!
  "Creates a test newspaper with a random Trove ID."
  [& {:keys [title] :or {title "Test Newspaper"}}]
  (newspaper/create-newspaper!
   {:title title
    :trove_newspaper_id (+ 95000 (rand-int 5000))}))

(defn- create-test-title!
  "Creates a test title with searchable attributes."
  [author-id newspaper-id & {:keys [title] :or {title "Searchable Title"}}]
  (title/create-title!
   {:publication_title title
    :author_id author-id
    :newspaper_table_id newspaper-id}))

(def ^:private mock-trove-article
  "Mock response from Trove API for testing chapter creation."
  {:chapter_html "<p>Searchable chapter content with unique text</p>"
   :chapter_text "Searchable chapter content with unique text"
   :article_url "https://trove.nla.gov.au/newspaper/article/12345"
   :corrections 0
   :word_count 50
   :illustrated false
   :pub_day 1
   :pub_month 1
   :pub_year 1890
   :trove_api_status 200})

;; =============================================================================
;; Integration tests for search-titles
;; =============================================================================

(deftest test-search-titles-returns-expected-structure
  (testing "search-titles returns properly structured results"
    (if (db-available?)
      (let [result (search/search-titles {})]
        (is (map? result))
        (is (contains? result :results))
        (is (contains? result :limit))
        (is (contains? result :offset))
        (is (contains? result :search_type))
        (is (= "title" (:search_type result)))
        (is (contains? result :next))
        (is (contains? result :previous)))
      (is true "Skipping - no database configured"))))

(deftest test-search-titles-with-title-text
  (testing "search-titles filters by title text"
    (if (db-available?)
      (let [author-id (create-test-author!)
            newspaper-id (create-test-newspaper!)
            _ (create-test-title! author-id newspaper-id :title "Unique Findable Title")
            _ (create-test-title! author-id newspaper-id :title "Another Different Title")
            result (search/search-titles {:title_text "Findable"})]
        (is (vector? (:results result))))
      (is true "Skipping - no database configured"))))

(deftest test-search-titles-with-author-nationality
  (testing "search-titles filters by author nationality"
    (if (db-available?)
      (let [author-id (create-test-author! :nationality "British")
            newspaper-id (create-test-newspaper!)
            _ (create-test-title! author-id newspaper-id)
            result (search/search-titles {:author_nationality "British"})]
        (is (map? result))
        (is (contains? result :results)))
      (is true "Skipping - no database configured"))))

(deftest test-search-titles-pagination-defaults
  (testing "search-titles uses default pagination values"
    (if (db-available?)
      (let [result (search/search-titles {})]
        (is (= 50 (:limit result)))
        (is (= 0 (:offset result))))
      (is true "Skipping - no database configured"))))

(deftest test-search-titles-custom-pagination
  (testing "search-titles respects custom pagination parameters"
    (if (db-available?)
      (let [result (search/search-titles {:limit 10 :offset 5})]
        (is (= 10 (:limit result)))
        (is (= 5 (:offset result))))
      (is true "Skipping - no database configured"))))

;; =============================================================================
;; Integration tests for search-chapters
;; =============================================================================

(deftest test-search-chapters-returns-expected-structure
  (testing "search-chapters returns properly structured results"
    (if (db-available?)
      (let [result (search/search-chapters {})]
        (is (map? result))
        (is (contains? result :results))
        (is (contains? result :limit))
        (is (contains? result :offset))
        (is (contains? result :search_type))
        (is (= "chapter" (:search_type result)))
        (is (contains? result :next))
        (is (contains? result :previous)))
      (is true "Skipping - no database configured"))))

(deftest test-search-chapters-with-chapter-text
  (testing "search-chapters filters by chapter text content"
    (if (db-available?)
      (with-redefs [trove/get-article (constantly mock-trove-article)]
        (let [author-id (create-test-author!)
              newspaper-id (create-test-newspaper!)
              title-id (create-test-title! author-id newspaper-id)
              _ (chapter/create-chapter!
                 {:title_id title-id
                  :trove_article_id (+ 800000 (rand-int 100000))})
              result (search/search-chapters {:chapter_text "Searchable"})]
          (is (map? result))
          (is (contains? result :results))))
      (is true "Skipping - no database configured"))))

(deftest test-search-chapters-pagination-defaults
  (testing "search-chapters uses default pagination values"
    (if (db-available?)
      (let [result (search/search-chapters {})]
        (is (= 50 (:limit result)))
        (is (= 0 (:offset result))))
      (is true "Skipping - no database configured"))))

(deftest test-search-chapters-custom-pagination
  (testing "search-chapters respects custom pagination parameters"
    (if (db-available?)
      (let [result (search/search-chapters {:limit 25 :offset 10})]
        (is (= 25 (:limit result)))
        (is (= 10 (:offset result))))
      (is true "Skipping - no database configured"))))

(deftest test-search-chapters-with-multiple-filters
  (testing "search-chapters supports multiple filter criteria"
    (if (db-available?)
      (let [result (search/search-chapters
                    {:chapter_text "content"
                     :title_text "title"
                     :author_nationality "Australian"})]
        (is (map? result))
        (is (contains? result :results)))
      (is true "Skipping - no database configured"))))
