#!/usr/bin/env bb

(require '[babashka.curl :as curl])
(require '[cheshire.core :as json])

(def base-url "http://localhost:3000/api/v1")

(println "\n=== Testing Authorization on Protected Endpoints ===\n")

;; Test without auth
(println "1. Testing POST /create/author WITHOUT authentication:")
(try
  (let [response (curl/post (str base-url "/create/author")
                            {:headers {"Content-Type" "application/json"}
                             :body (json/generate-string {:common_name "Test Author"})
                             :throw false})]
    (println "   Status:" (:status response))
    (if (= 401 (:status response))
      (println "   ✓ Correctly returns 401 Unauthorized")
      (println "   ✗ Expected 401, got" (:status response))))
  (catch Exception e
    (println "   Error:" (.getMessage e))))

;; Test with mock auth (for test mode)
(println "\n2. Testing POST /create/author WITH mock authentication:")
(try
  (let [response (curl/post (str base-url "/create/author")
                            {:headers {"Content-Type" "application/json"
                                       "Authorization" "Bearer mock-test-token"}
                             :body (json/generate-string {:common_name "Test Author"})
                             :throw false})]
    (println "   Status:" (:status response))
    (if (= 200 (:status response))
      (println "   ✓ Successfully authenticated and created author")
      (println "   Response body:" (:body response))))
  (catch Exception e
    (println "   Error:" (.getMessage e))))

;; Test auth test endpoint
(println "\n3. Testing /test endpoint (auth check):")
(try
  (println "   Without auth:")
  (let [response (curl/get (str base-url "/test") {:throw false})]
    (println "     Status:" (:status response))
    (if (= 401 (:status response))
      (println "     ✓ Correctly returns 401")
      (println "     ✗ Expected 401, got" (:status response))))

  (println "   With mock auth:")
  (let [response (curl/get (str base-url "/test")
                           {:headers {"Authorization" "Bearer mock-test-token"}
                            :throw false})]
    (println "     Status:" (:status response))
    (if (= 200 (:status response))
      (println "     ✓ Successfully authenticated")
      (println "     Response:" (:body response))))
  (catch Exception e
    (println "   Error:" (.getMessage e))))

(println "\n=== Test Complete ===")
