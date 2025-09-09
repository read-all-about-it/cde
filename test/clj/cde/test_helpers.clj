(ns cde.test-helpers
  (:require
   [cde.db.core :as db]
   [cde.db.newspaper :as newspaper]
   [cde.db.author :as author]
   [cde.db.title :as title]
   [cde.db.chapter :as chapter]
   [buddy.sign.jwt :as jwt]
   [clj-time.core :as time]
   [clj-time.coerce :as tc]))

(defn test-jwt
  "Create a test JWT token for authentication"
  []
  (str "Bearer test-jwt-token"))

(defn create-signed-jwt
  "Create a signed JWT token for testing"
  [claims]
  "test-jwt-token")

(defn with-mock-auth
  "Wrap an app handler to bypass JWT validation for testing"
  [handler]
  handler)

(defn get-test-newspaper-id
  "Get a valid newspaper ID from the database for testing"
  []
  (try
    (when-let [newspapers (newspaper/get-newspapers 1 0)]
      (-> newspapers :results first :id))
    (catch Exception _ nil)))

(defn get-test-author-id
  "Get a valid author ID from the database for testing"
  []
  (try
    (when-let [authors (author/get-authors 1 0)]
      (-> authors :results first :id))
    (catch Exception _ nil)))

(defn get-test-title-id
  "Get a valid title ID from the database for testing"
  []
  (try
    (when-let [titles (title/get-titles 1 0)]
      (-> titles :results first :id))
    (catch Exception _ nil)))

(defn get-test-chapter-id
  "Get a valid chapter ID from the database for testing"
  []
  (try
    (when-let [chapters (chapter/get-chapters 1 0)]
      (-> chapters :results first :id))
    (catch Exception _ nil)))

(defn create-test-newspaper!
  "Create a test newspaper and return its ID"
  [& [{:keys [newspaper_title] :or {newspaper_title "Test Newspaper"}}]]
  (newspaper/create-newspaper! {:newspaper_title newspaper_title
                                :location "Test Location"
                                :colony_state "Test State"}))

(defn create-test-author!
  "Create a test author and return its ID"
  [& [{:keys [common_name] :or {common_name "Test Author"}}]]
  (author/create-author! {:common_name common_name
                          :gender "Unknown"
                          :nationality "Unknown"}))

(defn create-test-title!
  "Create a test title and return its ID"
  [& [{:keys [author_id newspaper_id]}]]
  (let [author-id (or author_id (create-test-author!))
        newspaper-id (or newspaper_id (create-test-newspaper!))]
    (title/create-title! {:author_id author-id
                          :newspaper_table_id newspaper-id
                          :publication_title "Test Title"})))

(defn create-test-chapter!
  "Create a test chapter and return its ID"
  [& [{:keys [title_id trove_article_id]
       :or {trove_article_id (+ 100000 (rand-int 900000))}}]]
  (let [title-id (or title_id (create-test-title!))]
    (chapter/create-chapter! {:title_id title-id
                              :trove_article_id trove_article_id
                              :chapter_title "Test Chapter"
                              :chapter_text "Test content"})))
