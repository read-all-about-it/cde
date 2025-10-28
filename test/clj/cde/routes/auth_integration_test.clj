(ns cde.routes.auth-integration-test
  "Integration tests for authorization on protected endpoints"
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.handler :refer [app]]
   [cde.middleware.formats :as formats]
   [muuntaja.core :as m]))

;; Helper functions
(defn parse-response [response]
  (when (and response (:body response))
    (m/decode formats/instance "application/json" (:body response))))

(defn authenticated-request
  "Create an authenticated request with JWT token"
  [method path & [body]]
  (cond-> (mock/request method path)
    body (mock/json-body body)
    true (mock/header "Authorization" "Bearer mock-test-token")
    true (mock/header "Content-Type" "application/json")))

(defn unauthenticated-request
  "Create an unauthenticated request"
  [method path & [body]]
  (cond-> (mock/request method path)
    body (mock/json-body body)
    true (mock/header "Content-Type" "application/json")))

(deftest test-authorization-on-protected-endpoints
  (with-redefs [cde.config/env {:test-mode true}]
    (let [handler (app)]

      (testing "Protected endpoints require authentication"
        (testing "POST /api/v1/create/author without auth returns 401"
          (let [response (handler (unauthenticated-request :post "/api/v1/create/author"
                                                           {:common_name "Test Author"}))]
            (is (= 401 (:status response)))))

        (testing "POST /api/v1/create/author with auth returns success"
          (let [response (handler (authenticated-request :post "/api/v1/create/author"
                                                         {:common_name "Test Author"}))]
            ;; Should either succeed or fail with DB error (not auth error)
            (is (not= 401 (:status response)))))

        (testing "PUT /api/v1/author/:id without auth returns 401"
          (let [response (handler (unauthenticated-request :put "/api/v1/author/1"
                                                           {:common_name "Updated Author"}))]
            (is (= 401 (:status response)))))

        (testing "PUT /api/v1/author/:id with auth returns non-401"
          (let [response (handler (authenticated-request :put "/api/v1/author/1"
                                                         {:common_name "Updated Author"}))]
            ;; Should either succeed or fail with DB/not-found error (not auth error)
            (is (not= 401 (:status response)))))

        (testing "GET /api/v1/test without auth returns 401"
          (let [response (handler (unauthenticated-request :get "/api/v1/test"))]
            (is (= 401 (:status response)))))

        (testing "GET /api/v1/test with auth returns 200"
          (let [response (handler (authenticated-request :get "/api/v1/test"))]
            (is (= 200 (:status response)))
            (let [body (parse-response response)]
              (is (contains? body :message))
              (is (= "Hello, world!" (:message body)))))))

      (testing "Public endpoints work without authentication"
        (testing "GET /api/v1/authors is public"
          (let [response (handler (unauthenticated-request :get "/api/v1/authors"))]
            (is (= 200 (:status response)))))

        (testing "GET /api/v1/titles is public"
          (let [response (handler (unauthenticated-request :get "/api/v1/titles"))]
            (is (= 200 (:status response)))))

        (testing "GET /api/v1/chapters is public"
          (let [response (handler (unauthenticated-request :get "/api/v1/chapters"))]
            (is (= 200 (:status response)))))

        (testing "GET /api/v1/newspapers is public"
          (let [response (handler (unauthenticated-request :get "/api/v1/newspapers"))]
            (is (= 200 (:status response)))))))))
