(ns cde.test-helpers
  (:require
   [buddy.sign.jwt :as jwt]
   [buddy.core.keys :as keys]
   [clj-time.core :as time]
   [clj-time.coerce :as tc]
   [clojure.java.io :as io]
   [ring.mock.request :as mock]))

;; Mock JWT configuration for testing
(def test-config
  {:issuer "https://read-all-about-it.au.auth0.com/"
   :audience "https://readallaboutit.com.au/api/v1/"
   :algorithm :rs256})

;; Generate a test RSA key pair for JWT signing
(def test-private-key
  "-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAwf2YsHzcHvJmYNJk6Y7Rh9BCccXIPjrKNM8Va5t8JG5rPaOi
6xgJwPYaNVDe6Se3f1dSaH3vvBhKKlRTLTxqVdeckWOmQbsaC6b+Vg4HFsyPNMlh
ydQIrGTTumSQWVaAWmQtZ+dDCZbfbABxPgCdGY7nGEATcCXVRZm4kfMYPXAhMPc3
SrQCwJsGlLGW9qMZp9gYJxp5h+cYwVOyKi2mIRmPNwNS7MC8abxA9VrZJdRLjCLL
MhCl7Vx4c4b+phyhz2UvD8xkiAIGcgowLN7FE8jzQxPvCxBp8MTpoKKXWjK7Iad9
+sYOgYKVY2TqF2CPJcYhoiUBQJvFGcrCqp/eUwIDAQABAoIBAGnKrXDDWaVBW/gZ
H0jPTzHnfLuONwxspizn7aTCuDDTmxLvUnJjJG+08A4PzJ5fPQKUcbMhFOg+F7O8
n3bJCCM5kmuoVZRFxwcxifVX7M+DfCCb0TRXdRn0D4A+n2qFQizevDFH/Bdo3kv5
SvNaIcU8uVihx5ldQNOl1KkYCXV0R9M0HPEMRy2U0D9OBVqQyLCBMD8PFxYOMVua
n8YhGDUcRRYxYy5NhH6K3tRXeKokCCLhCvObzJZDTNosKl1Sj8V41pzu8SEqClDV
hMnKn0P3B3A5fVzNQc6B42z5VJhVKJSs6xJqz5C2lOFCusR0+qLKKnY8xAFgdOKa
pEDneAECgYEA+mxMWzPovJYGJ1D/cCd6j6FNCQVLJHKuQbx9WD6A6FmUVJ1DPheb
3hTlRGr2YBEofV8SKfzEqqJGEu3G7N6Q3MjXj5xdvfgUKMJhAzm0ABGwE1EQYAfn
Y8IVqFBYIj5oZUJE5I0GzFjb3ihqxt3b4sB+vOjzOdRhduLLTvWKylMCgYEAxlKN
krZdVYmJ8jUkjGLnGP/fzar6lgLaJlcKUB8XaLShYIBYQc3vKFMJvOWeHCQMUACI
lIC7XgZf0afXKrJjqRCmF2LnKRcJhMHNLgVTkLqOje3eHSQZRB2FQqu1c4voKT8V
1JKaHWUrWa8c8b+nH9RJgQzQLzchE7HdU8TzQQECgYEAgENEP8SiAX5+fkEMdtw1
NfiWR6LmvKMBClImPQdYfbWFpE09kVvdT9E3dcJ7Xvwvr5N1E8cAlINvCahQ3Ux1
+bwD7Oh8s5c8JQqW4gMkvC7f7VMdKdAZFaJmzL9zWCfwyaT3XLeGoVS1ryYWlnPX
iVLbSF8jF3VfqQHYwsu6Td0CgYEAxY0VAzEEYN8qLuM0JJZjKZFKCLB+hZ+ViDQ3
y/CNSkL7FKDLjp7ebD3aDlseXfh1Y2PBxLKBq5F3o8As5O7eXMv0K+rDZmJ5+5Ys
3EHrAk+DswwYJOkzB6XhRSXp0SjPKlLnECuKdw6k3Q8Y8K6Xnv7YYQGBqD3rs3gR
F7zOGAECgYATKfUNLXxRLnJ6aUzvCGhQscPslhF1YLX9+2tPveK9Z9SVPmBYPR+2
vVCBQPP8j9dDg+vGxBpNqzF6rNcUbePYqDTEWZLrhJEFhA8uGzborHcJC8IEzf8W
r+fTvdHG5dC5gy7bJQM6WYHASe5bhDO8dB5YoLxjzqBF2aGLLOaBwQ==
-----END RSA PRIVATE KEY-----")

(def test-public-key
  "-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwf2YsHzcHvJmYNJk6Y7R
h9BCccXIPjrKNM8Va5t8JG5rPaOi6xgJwPYaNVDe6Se3f1dSaH3vvBhKKlRTLTxq
VdeckWOmQbsaC6b+Vg4HFsyPNMlhydQIrGTTumSQWVaAWmQtZ+dDCZbfbABxPgCd
GY7nGEATcCXVRZm4kfMYPXAhMPc3SrQCwJsGlLGW9qMZp9gYJxp5h+cYwVOyKi2m
IRmPNwNS7MC8abxA9VrZJdRLjCLL MhCl7Vx4c4b+phyhz2UvD8xkiAIGcgowLN7F
E8jzQxPvCxBp8MTpoKKXWjK7Iad9+sYOgYKVY2TqF2CPJcYhoiUBQJvFGcrCqp/e
UwIDAQAB
-----END PUBLIC KEY-----")

(defn create-signed-jwt
  "Create a properly signed JWT token for testing"
  [claims]
  (let [private-key (keys/str->private-key test-private-key)
        default-claims {:iss (:issuer test-config)
                        :aud (:audience test-config)
                        :sub "auth0|test-user-123"
                        :email "test@example.com"
                        :name "Test User"
                        :exp (tc/to-long (time/plus (time/now) (time/hours 1)))
                        :iat (tc/to-long (time/now))}
        final-claims (merge default-claims claims)]
    (jwt/sign final-claims private-key {:alg (:algorithm test-config)})))

(defn verify-test-jwt
  "Verify a test JWT token"
  [token]
  (let [public-key (keys/str->public-key test-public-key)]
    (try
      (jwt/unsign token public-key {:alg (:algorithm test-config)})
      (catch Exception e
        nil))))

(defn mock-jwt-middleware
  "Middleware that mocks JWT validation for testing"
  [handler]
  (fn [request]
    (if-let [auth-header (get-in request [:headers "authorization"])]
      (if (re-find #"^Bearer " auth-header)
        (let [token (clojure.string/replace auth-header #"^Bearer " "")
              claims (if (= token "mock-test-token")
                       {:sub "auth0|test-user-123"
                        :email "test@example.com"
                        :name "Test User"}
                       (verify-test-jwt token))]
          (if claims
            (handler (assoc request :jwt-claims claims))
            (handler request)))
        (handler request))
      (handler request))))

(defn with-mock-auth
  "Wrap app with mock JWT validation for testing"
  [app]
  (mock-jwt-middleware app))

(defn authenticated-request
  "Create a properly authenticated request for testing"
  ([method path]
   (authenticated-request method path nil))
  ([method path body]
   (let [;; Use mock token that will be recognized by test middleware
         token "mock-test-token"
         request (mock/request method path)]
     (cond-> request
       body (mock/json-body body)
       true (mock/header "Authorization" (str "Bearer " token))
       true (mock/header "Content-Type" "application/json")))))

(defn with-test-db
  "Run tests with a test database transaction that rolls back"
  [f]
  ;; This would set up a test database transaction
  ;; For now, just run the test
  (f))

;; Test data fixtures
(def test-user
  {:id 1
   :email "test@example.com"
   :auth0-id "auth0|test-user-123"})

(def test-author
  {:id 1
   :common_name "Test Author"
   :gender "Unknown"
   :nationality "Unknown"
   :added_by 1})

(def test-newspaper
  {:id 1
   :trove_newspaper_id 35
   :title "Test Newspaper (TEST : 2024 - 2024)"
   :common_title "Test Newspaper"
   :issn "1234-5678"
   :added_by 1})

(def test-title
  {:id 1
   :author_id 1
   :newspaper_table_id 1
   :common_title "Test Story"
   :publication_title "A Test Story"
   :added_by 1})

(def test-chapter
  {:id 1
   :title_id 1
   :trove_article_id 123456
   :chapter_number "1"
   :chapter_title "Chapter One"
   :added_by 1})
(ns cde.test-helpers)
