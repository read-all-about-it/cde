(ns cde.routes-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.handler :as handler]
   [cde.routes.services :as services]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start)
    (f)
    (mount/stop)))

(deftest test-routes-structure
  (testing "Service routes are created properly"
    (let [routes (services/service-routes)]
      (is (vector? routes))
      (is (= "/api" (first routes)))))

  (testing "Handler app works"
    (let [app (handler/app)
          response (app (mock/request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "API route directly"
    (let [app (handler/app)
          response (app (mock/request :get "/api/v1/platform/statistics"))]
      (println "Response status:" (:status response))
      (when (not= 200 (:status response))
        (println "Response body:" (when (:body response)
                                    (try (slurp (:body response))
                                         (catch Exception _ (:body response))))))
      (is (= 200 (:status response))))))
