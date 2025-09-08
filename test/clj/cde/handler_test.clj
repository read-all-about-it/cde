(ns cde.handler-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :refer :all]
   [cde.handler :refer :all]
   [cde.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'cde.config/env
                 #'cde.handler/app-routes)
    (f)))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "API endpoints"
    (testing "public endpoint - platform statistics"
      (let [response ((app) (request :get "/api/v1/platform/statistics"))]
        (is (= 200 (:status response)))))

    (testing "public endpoint - newspapers listing"
      (let [response ((app) (request :get "/api/v1/newspapers"))]
        (is (= 200 (:status response)))))

    (testing "protected endpoint without auth"
      (let [response ((app) (-> (request :post "/api/v1/create/author")
                                (json-body {:common_name "Test Author"})
                                (header "Content-Type" "application/json")))]
        (is (= 401 (:status response)))))))
