(ns cde.debug-test
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.handler :as handler]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (println "Starting mount components...")
    (mount/start #'cde.config/env
                 #'cde.handler/app-routes
                 #'cde.db.core/*db*)
    (println "Mount started successfully")
    (println "app-routes value:" @#'cde.handler/app-routes)
    (f)
    (mount/stop)))

(deftest test-debug-routes
  (testing "Debug route availability"
    (println "\n=== Testing route availability ===")
    (let [app (handler/app)]
      (println "App created")

      ;; Test home route
      (let [response (app (mock/request :get "/"))]
        (println "Home route status:" (:status response)))

      ;; Test API route directly
      (let [response (app (mock/request :get "/api/v1/platform/statistics"))]
        (println "API route status:" (:status response))
        (when (not= 200 (:status response))
          (println "Response body:" (when (:body response)
                                      (try
                                        (slurp (:body response))
                                        (catch Exception e
                                          (str "Error reading body: " (.getMessage e))))))))

      ;; Check if the app-routes is a function
      (println "app-routes type:" (type @#'cde.handler/app-routes))
      (println "app type:" (type app))

      (is (= 200 (:status (app (mock/request :get "/"))))))))
(ns cde.debug-test)