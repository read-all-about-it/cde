(ns cde.test-helpers
  "Test helper functions for authentication and database fixtures.

  Provides utilities for:
  - Creating test JWT tokens compatible with test-mode authentication
  - Mocking authentication middleware for handler tests
  - Creating and retrieving test database records

  Note: For test-mode authentication to work, ensure `:test-mode true`
  is set in the test configuration (test-config.edn)."
  (:require
   [cde.db.core :as db]
   [cde.db.newspaper :as newspaper]
   [cde.db.author :as author]
   [cde.db.title :as title]
   [cde.db.chapter :as chapter]
   [buddy.sign.jwt :as jwt]
   [clj-time.core :as time]
   [clj-time.coerce :as tc]))

(def ^:private default-test-claims
  "Default JWT claims used for test authentication."
  {"sub" "auth0|test-user"
   "email" "test@example.com"
   "name" "Test User"})

(defn test-jwt
  "Creates a test JWT Authorization header value.

  Returns a string formatted as 'Bearer mock-test-token' which is
  recognized by the middleware when running in test mode.

  Returns: String suitable for the Authorization header."
  []
  "Bearer mock-test-token")

(defn create-signed-jwt
  "Creates a test JWT token string for testing.

  When running in test mode (`:test-mode true` in env), the middleware
  recognizes tokens prefixed with `test-` or equal to `mock-test-token`.
  This function creates tokens compatible with that test mode.

  Arguments:
  - `claims` - Map of JWT claims (optional, uses defaults if key claims missing)
    - `\"sub\"` - Subject (user ID), defaults to \"auth0|test-user\"
    - `\"email\"` - User email, defaults to \"test@example.com\"
    - `\"name\"` - User name, defaults to \"Test User\"

  Returns: JWT token string (without 'Bearer ' prefix).

  Example:
    (create-signed-jwt {\"sub\" \"auth0|custom-user\"})
    ;; => \"test-custom-user\" (recognized in test mode)"
  [claims]
  (let [merged-claims (merge default-test-claims claims)
        sub (get merged-claims "sub")]
    ;; Extract user identifier from sub claim for test token
    ;; If sub is like "auth0|user-id", use "test-user-id"
    ;; Otherwise use the standard mock token
    (if (and sub (re-find #"auth0\|(.+)" sub))
      (str "test-" (second (re-find #"auth0\|(.+)" sub)))
      "mock-test-token")))

(defn with-mock-auth
  "Wraps a Ring handler to inject mock JWT claims for testing.

  This bypasses the JWT validation middleware by directly injecting
  `:jwt-claims` into the request. Use this when you need to test
  handlers that require authentication without going through the
  full middleware stack.

  Arguments:
  - `handler` - Ring handler function to wrap
  - `claims` - Optional map of JWT claims to inject (uses defaults if nil)

  Returns: Wrapped Ring handler that injects mock authentication.

  Example:
    (let [wrapped (with-mock-auth my-handler)]
      (wrapped (mock/request :get \"/protected-route\")))

    ;; With custom claims:
    (let [wrapped (with-mock-auth my-handler {\"sub\" \"auth0|admin\"})]
      (wrapped request))"
  ([handler]
   (with-mock-auth handler nil))
  ([handler claims]
   (let [mock-claims (merge default-test-claims claims)]
     (fn [request]
       (handler (assoc request
                       :jwt-claims mock-claims
                       :user-id (get mock-claims "sub")
                       :user-email (get mock-claims "email")))))))

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
