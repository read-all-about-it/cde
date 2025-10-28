(ns cde.middleware-test
  (:require
   [clojure.test :refer :all]
   [cde.middleware :as mw]
   [ring.mock.request :as mock]
   [buddy.sign.jwt :as jwt]
   [clj-time.core :as time]
   [clj-time.coerce :as tc]))

;; Test helpers
(def test-secret "test-secret-key-for-testing-only")
(def test-issuer "https://read-all-about-it.au.auth0.com/")

(defn create-test-token
  "Creates a test JWT token with given claims"
  [claims]
  (jwt/sign claims test-secret {:alg :hs256}))

(defn create-valid-auth0-token
  "Creates a token that mimics Auth0 structure"
  []
  (create-test-token
   {:sub "auth0|123456789"
    :email "test@example.com"
    :name "Test User"
    :iss test-issuer
    :aud "https://readallaboutit.com.au/api/v1/"
    :exp (tc/to-long (time/plus (time/now) (time/hours 1)))
    :iat (tc/to-long (time/now))}))

(defn create-expired-token
  "Creates an expired JWT token"
  []
  (create-test-token
   {:sub "auth0|123456789"
    :email "test@example.com"
    :exp (tc/to-long (time/minus (time/now) (time/hours 1)))
    :iat (tc/to-long (time/minus (time/now) (time/hours 2)))}))

(defn mock-handler
  "A mock handler that returns success with user info if authenticated"
  [request]
  {:status 200
   :body {:message "Success"
          :user-id (:user-id request)
          :user-email (:user-email request)}})

(defn mock-error-handler
  "A mock handler that always returns an error"
  [_]
  (throw (Exception. "Test error")))

;; Tests for JWT validation middleware

(deftest test-check-auth0-jwt
  (testing "Request with valid JWT token"
    (let [handler (mw/check-auth0-jwt mock-handler)
          request (-> (mock/request :get "/test")
                      (assoc :jwt-claims {"sub" "auth0|123"
                                          "email" "test@example.com"}))]
      (let [response (handler request)]
        (is (= 200 (:status response)))
        (is (= "auth0|123" (get-in response [:body :user-id])))
        (is (= "test@example.com" (get-in response [:body :user-email]))))))

  (testing "Request without JWT token"
    (let [handler (mw/check-auth0-jwt mock-handler)
          request (mock/request :get "/test")]
      (let [response (handler request)]
        (is (= 401 (:status response)))
        (is (re-find #"Authentication required" (str response))))))

  (testing "Request with empty JWT claims"
    (let [handler (mw/check-auth0-jwt mock-handler)
          request (-> (mock/request :get "/test")
                      (assoc :jwt-claims nil))]
      (let [response (handler request)]
        (is (= 401 (:status response)))))))

(deftest test-wrap-https-redirect
  (testing "HTTP request gets redirected to HTTPS"
    (let [handler (mw/wrap-https-redirect mock-handler)
          request (-> (mock/request :get "/test")
                      (assoc-in [:headers "x-forwarded-proto"] "http")
                      (assoc :server-name "example.com")
                      (assoc :uri "/api/test"))]
      (let [response (handler request)]
        (is (= 301 (:status response)))
        (is (= "https://example.com/api/test"
               (get-in response [:headers "Location"]))))))

  (testing "HTTPS request passes through"
    (let [handler (mw/wrap-https-redirect mock-handler)
          request (-> (mock/request :get "/test")
                      (assoc-in [:headers "x-forwarded-proto"] "https"))]
      (let [response (handler request)]
        (is (= 200 (:status response))))))

  (testing "Request without x-forwarded-proto passes through"
    (let [handler (mw/wrap-https-redirect mock-handler)
          request (mock/request :get "/test")]
      (let [response (handler request)]
        (is (= 200 (:status response)))))))

(deftest test-wrap-internal-error
  (testing "Handler exceptions are caught and return 500"
    (let [handler (mw/wrap-internal-error mock-error-handler)
          request (mock/request :get "/test")]
      (let [response (handler request)]
        (is (= 500 (:status response)))
        (is (re-find #"Something very bad has happened" (str response))))))

  (testing "Successful requests pass through"
    (let [handler (mw/wrap-internal-error mock-handler)
          request (mock/request :get "/test")]
      (let [response (handler request)]
        (is (= 200 (:status response)))))))
