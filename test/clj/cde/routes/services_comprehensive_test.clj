(ns cde.routes.services-comprehensive-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.handler :refer [app]]
   [cde.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mount.core :as mount]
   [cde.config :refer [env]]
   [cde.db.core :as db]
   [cde.test-helpers :as th]))

;; Test setup
(use-fixtures
  :once
  (fn [f]
    (mount/start #'cde.config/env
                 #'cde.handler/app-routes
                 #'cde.db.core/*db*)
    (f)
    (mount/stop)))

;; No per-test fixtures needed for now
;; Could add database transaction rollback here if needed

;; Helper functions
(defn parse-body [response]
  (when (and response (:body response))
    (m/decode-response-body response)))

(defn test-endpoint
  "Test an endpoint with expected status and optional body checks"
  [method path expected-status & [{:keys [query-params body-checks auth?]}]]
  (testing (str method " " path)
    (let [request (cond-> (mock/request method path)
                    query-params (mock/query-string query-params)
                    auth? (mock/header "Authorization" (th/test-jwt)))
          response ((app) request)
          body (parse-body response)]
      (is (= expected-status (:status response))
          (str "Expected status " expected-status " but got " (:status response)))
      (when body-checks
        (doseq [[k v] body-checks]
          (if (fn? v)
            (is (v (get body k)) (str "Check failed for key " k))
            (is (= v (get body k)) (str "Expected " v " for key " k))))))))

;; Platform & Options Tests
(deftest test-platform-endpoints
  (test-endpoint :get "/api/v1/platform/statistics" 200
                 {:body-checks {:total_chapters number?
                                :total_titles number?
                                :total_authors number?
                                :total_newspapers number?}})

  (test-endpoint :get "/api/v1/platform/search-options" 200
                 {:body-checks {:author-nationalities coll?
                                :author-genders coll?}})

  (test-endpoint :get "/api/v1/options/newspapers" 200
                 {:body-checks {nil coll?}})

  (test-endpoint :get "/api/v1/options/authors" 200
                 {:body-checks {nil coll?}}))

;; Search Endpoints Tests
(deftest test-search-endpoints
  (test-endpoint :get "/api/v1/search/titles" 200
                 {:query-params {:title_text "test"}
                  :body-checks {:results coll?
                                :search_type #(= "title" %)
                                :limit number?
                                :offset number?}})

  (test-endpoint :get "/api/v1/search/chapters" 200
                 {:query-params {:chapter_text "test"}
                  :body-checks {:results coll?
                                :search_type #(= "chapter" %)
                                :limit number?
                                :offset number?}})

  (testing "Search newspapers requires trove_newspaper_id"
    (test-endpoint :get "/api/v1/search/newspapers" 400
                   {:query-params {}}))

  (test-endpoint :get "/api/v1/search/newspapers" 200
                 {:query-params {:trove-newspaper-id 35}
                  :body-checks {nil #(or (coll? %) (nil? %))}}))

;; Newspaper CRUD Tests
(deftest test-newspaper-endpoints
  (test-endpoint :get "/api/v1/newspapers" 200
                 {:query-params {:limit 10 :offset 0}
                  :body-checks {:results coll?
                                :next #(or (string? %) (nil? %))
                                :previous #(or (string? %) (nil? %))}})

  (when-let [newspaper-id (th/get-test-newspaper-id)]
    (test-endpoint :get (str "/api/v1/newspaper/" newspaper-id) 200
                   {:body-checks {:id number?
                                  :newspaper_title string?}})

    (test-endpoint :get (str "/api/v1/newspaper/" newspaper-id "/titles") 200
                   {:body-checks {nil coll?}}))

  (testing "Create newspaper requires auth"
    (test-endpoint :post "/api/v1/create/newspaper" 401
                   {:body {:newspaper_title "Test Paper"}})))

;; Author CRUD Tests
(deftest test-author-endpoints
  (test-endpoint :get "/api/v1/authors" 200
                 {:query-params {:limit 10 :offset 0}
                  :body-checks {:results coll?
                                :next #(or (string? %) (nil? %))
                                :previous #(or (string? %) (nil? %))}})

  (test-endpoint :get "/api/v1/author-nationalities" 200
                 {:body-checks {nil coll?}})

  (test-endpoint :get "/api/v1/author-genders" 200
                 {:body-checks {nil coll?}})

  (when-let [author-id (th/get-test-author-id)]
    (test-endpoint :get (str "/api/v1/author/" author-id) 200
                   {:body-checks {:id number?
                                  :common_name string?}})

    (test-endpoint :get (str "/api/v1/author/" author-id "/titles") 200
                   {:body-checks {nil coll?}})

    (testing "Update author requires auth"
      (test-endpoint :put (str "/api/v1/author/" author-id) 401
                     {:body {:common_name "Updated Name"}})))

  (testing "Create author requires auth"
    (test-endpoint :post "/api/v1/create/author" 401
                   {:body {:common_name "Test Author"}})))

;; Title CRUD Tests
(deftest test-title-endpoints
  (test-endpoint :get "/api/v1/titles" 200
                 {:query-params {:limit 10 :offset 0}
                  :body-checks {:results coll?
                                :next #(or (string? %) (nil? %))
                                :previous #(or (string? %) (nil? %))}})

  (when-let [title-id (th/get-test-title-id)]
    (test-endpoint :get (str "/api/v1/title/" title-id) 200
                   {:body-checks {:id number?}})

    (test-endpoint :get (str "/api/v1/title/" title-id "/chapters") 200
                   {:body-checks {nil coll?}})

    (testing "Update title requires auth"
      (test-endpoint :put (str "/api/v1/title/" title-id) 401
                     {:body {:publication_title "Updated Title"}})))

  (testing "Create title requires auth"
    (test-endpoint :post "/api/v1/create/title" 401
                   {:body {:author_id 1
                           :newspaper_table_id 1}})))

;; Chapter CRUD Tests
(deftest test-chapter-endpoints
  (test-endpoint :get "/api/v1/chapters" 200
                 {:query-params {:limit 10 :offset 0}
                  :body-checks {:results coll?
                                :next #(or (string? %) (nil? %))
                                :previous #(or (string? %) (nil? %))}})

  (when-let [chapter-id (th/get-test-chapter-id)]
    (test-endpoint :get (str "/api/v1/chapter/" chapter-id) 200
                   {:body-checks {:id number?}})

    (testing "Update chapter requires auth"
      (test-endpoint :put (str "/api/v1/chapter/" chapter-id) 401
                     {:body {:chapter_title "Updated Chapter"}})))

  (testing "Create chapter requires auth"
    (test-endpoint :post "/api/v1/create/chapter" 401
                   {:body {:title_id 1
                           :trove_article_id 123456}})))

;; Trove Integration Tests
(deftest test-trove-endpoints
  (test-endpoint :get "/api/v1/trove/exists/chapter/123456" 200
                 {:body-checks {:exists boolean?
                                :trove_article_id #(= 123456 %)
                                :chapter_id #(or (number? %) (nil? %))}})

  (test-endpoint :get "/api/v1/trove/exists/newspaper/35" 200
                 {:body-checks {:exists boolean?
                                :trove_newspaper_id #(= 35 %)
                                :newspaper_table_id #(or (number? %) (nil? %))}})

  ;; Note: Actual Trove API calls would require mocking
  (testing "Trove newspaper passthrough"
    (with-redefs [cde.trove/get-newspaper (constantly {:title "Test Paper"
                                                       :trove_api_status 200})]
      (test-endpoint :get "/api/v1/trove/newspaper/35" 200
                     {:body-checks {:title #(= "Test Paper" %)}})))

  (testing "Trove chapter passthrough"
    (with-redefs [cde.trove/get-article (constantly {:title "Test Article"
                                                     :trove_api_status 200})]
      (test-endpoint :get "/api/v1/trove/chapter/123456" 200
                     {:body-checks {:title #(= "Test Article" %)}}))))

;; User & Auth Tests
(deftest test-user-endpoints
  (testing "Test endpoint requires auth"
    (test-endpoint :get "/api/v1/test" 401))

  (testing "User creation/retrieval"
    (test-endpoint :get "/api/v1/user" 200
                   {:query-params {:email "test@example.com"}
                    :body-checks {:email #(= "test@example.com" %)
                                  :id number?}})))

;; Validation Tests
(deftest test-request-validation
  (testing "Invalid query parameters are rejected"
    (test-endpoint :get "/api/v1/titles" 400
                   {:query-params {:limit "invalid"}}))

  (testing "Missing required fields in body"
    (with-redefs [cde.middleware/check-auth0-jwt identity]
      (let [response ((app) (-> (mock/request :post "/api/v1/create/title")
                                (mock/json-body {:author_id 1}) ; Missing newspaper_table_id
                                (mock/header "Content-Type" "application/json")))]
        (is (= 400 (:status response))))))

  (testing "Invalid path parameters"
    (test-endpoint :get "/api/v1/author/invalid" 400)))

;; Pagination Tests
(deftest test-pagination
  (doseq [endpoint ["/api/v1/newspapers" "/api/v1/authors"
                    "/api/v1/titles" "/api/v1/chapters"]]
    (testing (str "Pagination for " endpoint)
      (test-endpoint :get endpoint 200
                     {:query-params {:limit 5 :offset 10}
                      :body-checks {:limit #(= 5 %)
                                    :offset #(= 10 %)
                                    :results #(<= (count %) 5)}}))))

;; Error Handling Tests
(deftest test-error-handling
  (testing "Non-existent resource returns 404"
    (test-endpoint :get "/api/v1/author/999999" 404
                   {:body-checks {:message string?}}))

  (testing "Database errors are handled gracefully"
    (with-redefs [cde.db.author/get-authors (fn [_ _] (throw (Exception. "DB Error")))]
      (test-endpoint :get "/api/v1/authors" 404
                     {:body-checks {:message #(re-find #"DB Error" %)}}))))

;; Response Format Tests
(deftest test-response-formats
  (testing "JSON response format"
    (let [response ((app) (mock/request :get "/api/v1/platform/statistics"))]
      (is (re-find #"application/json" (get-in response [:headers "Content-Type"])))))

  (testing "Swagger documentation available"
    (test-endpoint :get "/api/swagger.json" 200))

  (testing "API docs UI available"
    (let [response ((app) (mock/request :get "/api/api-docs/index.html"))]
      (is (= 200 (:status response))))))
