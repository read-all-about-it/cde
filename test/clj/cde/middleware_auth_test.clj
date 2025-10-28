(ns cde.middleware-auth-test
  "Test authentication middleware behavior"
  (:require
   [clojure.test :refer :all]
   [ring.mock.request :as mock]
   [cde.middleware :as mw]))

(deftest test-wrap-auth0-middleware
  (testing "wrap-auth0 middleware in test mode"
    ;; Create a test handler that returns JWT claims if present
    (let [test-handler (fn [req]
                         {:status 200
                          :body {:jwt-claims (:jwt-claims req)
                                 :user-id (:user-id req)
                                 :user-email (:user-email req)}})
          wrapped-handler (mw/wrap-auth0 test-handler)]

      (testing "Without authorization header"
        (let [request (mock/request :get "/test")
              response (wrapped-handler request)]
          (is (= 200 (:status response)))
          (is (nil? (get-in response [:body :jwt-claims])))))

      (testing "With mock token in test mode"
        (with-redefs [cde.config/env {:test-mode true}]
          (let [request (-> (mock/request :get "/test")
                            (mock/header "Authorization" "Bearer mock-test-token"))
                response (wrapped-handler request)]
            (is (= 200 (:status response)))
            (is (= "auth0|test-user" (get-in response [:body :jwt-claims "sub"])))
            (is (= "test@example.com" (get-in response [:body :jwt-claims "email"]))))))

      (testing "With test- prefixed token in test mode"
        (with-redefs [cde.config/env {:test-mode true}]
          (let [request (-> (mock/request :get "/test")
                            (mock/header "Authorization" "Bearer test-user-123"))
                response (wrapped-handler request)]
            (is (= 200 (:status response)))
            (is (= "auth0|test-user-123" (get-in response [:body :jwt-claims "sub"])))
            (is (= "test@example.com" (get-in response [:body :jwt-claims "email"])))))))))

(deftest test-check-auth0-jwt-middleware
  (testing "check-auth0-jwt middleware"
    (let [test-handler (fn [req]
                         {:status 200
                          :body {:user-id (:user-id req)
                                 :user-email (:user-email req)}})
          protected-handler (mw/check-auth0-jwt test-handler)]

      (testing "Without JWT claims - returns 401"
        (let [request (mock/request :get "/protected")
              response (protected-handler request)]
          (is (= 401 (:status response)))))

      (testing "With JWT claims - passes through"
        (let [request (-> (mock/request :get "/protected")
                          (assoc :jwt-claims {"sub" "auth0|user123"
                                              "email" "user@example.com"}))
              response (protected-handler request)]
          (is (= 200 (:status response)))
          (is (= "auth0|user123" (get-in response [:body :user-id])))
          (is (= "user@example.com" (get-in response [:body :user-email]))))))))

(deftest test-middleware-composition
  (testing "Full middleware stack with auth"
    (let [test-handler (fn [req]
                         {:status 200
                          :body {:authenticated (boolean (:jwt-claims req))
                                 :user-id (:user-id req)}})
          ;; Apply both wrap-auth0 and check-auth0-jwt
          protected-handler (-> test-handler
                                (mw/check-auth0-jwt)
                                (mw/wrap-auth0))]

      (testing "Without auth header - returns 401"
        (with-redefs [cde.config/env {:test-mode true}]
          (let [request (mock/request :post "/api/create/something")
                response (protected-handler request)]
            (is (= 401 (:status response))))))

      (testing "With valid mock token - allows access"
        (with-redefs [cde.config/env {:test-mode true}]
          (let [request (-> (mock/request :post "/api/create/something")
                            (mock/header "Authorization" "Bearer mock-test-token"))
                response (protected-handler request)]
            (is (= 200 (:status response)))
            (is (= true (get-in response [:body :authenticated])))
            (is (= "auth0|test-user" (get-in response [:body :user-id])))))))))
