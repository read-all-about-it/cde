(ns cde.handler-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.handler :refer [app app-routes]]
   [cde.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mount.core :as mount]
   [cde.test-fixtures :refer [with-test-db]]))

(defn parse-json [body]
  (when body
    (m/decode formats/instance "application/json" body)))

(use-fixtures :once with-test-db)

(deftest test-app
  (testing "main route"
    (let [handler (app)
          response (handler (mock/request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [handler (app)
          response (handler (mock/request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "API endpoints"
    (let [handler (app)]
      (testing "public endpoint - platform statistics"
        (let [response (handler (mock/request :get "/api/v1/platform/statistics"))]
          (is (= 200 (:status response)))
          (when (= 200 (:status response))
            (let [body (parse-json (:body response))]
              (is (contains? body :chapter-count))
              (is (contains? body :title-count))
              (is (contains? body :author-count))
              (is (contains? body :newspaper-count))))))

      (testing "public endpoint - newspapers listing"
        (let [response (handler (mock/request :get "/api/v1/newspapers"))]
          (is (= 200 (:status response)))
          (when (= 200 (:status response))
            (let [body (parse-json (:body response))]
              (is (contains? body :results))
              (is (contains? body :limit))
              (is (contains? body :offset))))))

      (testing "public endpoint - authors listing"
        (let [response (handler (mock/request :get "/api/v1/authors"))]
          (is (= 200 (:status response)))
          (when (= 200 (:status response))
            (let [body (parse-json (:body response))]
              (is (contains? body :results))
              (is (contains? body :limit))
              (is (contains? body :offset))))))

      (testing "public endpoint - titles listing"
        (let [response (handler (mock/request :get "/api/v1/titles"))]
          (is (= 200 (:status response)))
          (when (= 200 (:status response))
            (let [body (parse-json (:body response))]
              (is (contains? body :results))
              (is (contains? body :limit))
              (is (contains? body :offset))))))

      (testing "public endpoint - chapters listing"
        (let [response (handler (mock/request :get "/api/v1/chapters"))]
          (is (= 200 (:status response)))
          (when (= 200 (:status response))
            (let [body (parse-json (:body response))]
              (is (contains? body :results))
              (is (contains? body :limit))
              (is (contains? body :offset))))))

      (testing "protected endpoint without auth should return 401"
        (let [body-str (m/encode formats/instance "application/json" {:common_name "Test Author"})
              response (handler (-> (mock/request :post "/api/v1/create/author")
                                    (mock/header "Content-Type" "application/json")
                                    (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str "UTF-8")))))]
          (is (= 401 (:status response))))))))
