(ns cde.routes.services-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.handler :refer [app]]
   [cde.middleware.formats :as formats]
   [muuntaja.core :as m]
   [cde.test-fixtures :refer [with-test-db with-rollback]]))

;; Test setup
(use-fixtures :once with-test-db)
(use-fixtures :each with-rollback)

;; Helper functions
(defn parse-response [response]
  (when (and response (:body response))
    (m/decode formats/instance "application/json" (:body response))))

(defn create-test-jwt
  "Create a mock JWT token for testing"
  [& {:keys [sub email]
      :or {sub "auth0|test123"
           email "test@example.com"}}]
  (str "Bearer mock-jwt-token-" sub))

(defn authenticated-request
  "Create an authenticated request with JWT token"
  [method path & [body]]
  (cond-> (mock/request method path)
    body (mock/json-body body)
    true (mock/header "Authorization" (create-test-jwt))
    true (mock/header "Content-Type" "application/json")))

;; Tests for public endpoints
(deftest test-public-endpoints
  (testing "GET /api/v1/platform/statistics - public endpoint"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/platform/statistics"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (map? body))
        (is (contains? body :chapter-count))
        (is (contains? body :title-count))
        (is (contains? body :author-count))
        (is (contains? body :newspaper-count)))))

  (testing "GET /api/v1/newspapers - public listing"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/newspapers?limit=10&offset=0"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (contains? body :limit))
        (is (contains? body :offset))
        (is (= 10 (:limit body)))
        (is (= 0 (:offset body))))))

  (testing "GET /api/v1/authors - public listing"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/authors?limit=10&offset=0"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (contains? body :limit))
        (is (contains? body :offset)))))

  (testing "GET /api/v1/titles - public listing"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/titles?limit=10&offset=0"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (contains? body :limit))
        (is (contains? body :offset)))))

  (testing "GET /api/v1/chapters - public listing"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/chapters?limit=10&offset=0"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (contains? body :limit))
        (is (contains? body :offset))))))

;; Tests for protected endpoints without authentication
(deftest test-protected-endpoints-without-auth
  (testing "POST /api/v1/create/author - requires authentication"
    (let [handler (app)
          body-str (m/encode formats/instance "application/json" {:common_name "Test Author"})
          response (handler (-> (mock/request :post "/api/v1/create/author")
                                (mock/header "Content-Type" "application/json")
                                (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
      (is (= 401 (:status response)))))

  (testing "POST /api/v1/create/title - requires authentication"
    (let [handler (app)
          body-str (m/encode formats/instance "application/json" {:author_id 1 :newspaper_table_id 1})
          response (handler (-> (mock/request :post "/api/v1/create/title")
                                (mock/header "Content-Type" "application/json")
                                (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
      (is (= 401 (:status response)))))

  (testing "POST /api/v1/create/chapter - requires authentication"
    (let [handler (app)
          body-str (m/encode formats/instance "application/json" {:title_id 1 :trove_article_id 123456})
          response (handler (-> (mock/request :post "/api/v1/create/chapter")
                                (mock/header "Content-Type" "application/json")
                                (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
      (is (= 401 (:status response)))))

  (testing "PUT /api/v1/author/1 - requires authentication"
    (let [handler (app)
          body-str (m/encode formats/instance "application/json" {:common_name "Updated Name"})
          response (handler (-> (mock/request :put "/api/v1/author/1")
                                (mock/header "Content-Type" "application/json")
                                (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
      (is (= 401 (:status response)))))

  (testing "PUT /api/v1/title/1 - requires authentication"
    (let [handler (app)
          body-str (m/encode formats/instance "application/json" {:common_title "Updated Title"})
          response (handler (-> (mock/request :put "/api/v1/title/1")
                                (mock/header "Content-Type" "application/json")
                                (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
      (is (= 401 (:status response)))))

  (testing "PUT /api/v1/chapter/1 - requires authentication"
    (let [handler (app)
          body-str (m/encode formats/instance "application/json" {:chapter_title "Updated Chapter"})
          response (handler (-> (mock/request :put "/api/v1/chapter/1")
                                (mock/header "Content-Type" "application/json")
                                (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
      (is (= 401 (:status response))))))

;; Tests for search functionality
(deftest test-search-endpoints
  (testing "Search titles"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/search/titles?title_text=test"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (contains? body :search_type))
        (is (= "title" (:search_type body))))))

  (testing "Search chapters"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/search/chapters?chapter_text=test"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (contains? body :search_type))
        (is (= "chapter" (:search_type body))))))

  (testing "Search with pagination"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/search/titles?title_text=test&limit=5&offset=10"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :results))
        (is (= 5 (:limit body)))
        (is (= 10 (:offset body)))))))

;; Tests for Trove integration
(deftest test-trove-endpoints
  (testing "Check if Trove article exists"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/trove/exists/chapter/123456"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :exists))
        (is (boolean? (:exists body)))
        (is (= 123456 (:trove_article_id body))))))

  (testing "Check if Trove newspaper exists"
    (let [handler (app)
          response (handler (mock/request :get "/api/v1/trove/exists/newspaper/35"))]
      (is (= 200 (:status response)))
      (let [body (parse-response response)]
        (is (contains? body :exists))
        (is (= 35 (:trove_newspaper_id body)))))))
