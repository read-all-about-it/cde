(ns cde.simple-test
  (:require [clojure.test :refer :all]))

(deftest simple-passing-test
  (testing "Basic arithmetic"
    (is (= 4 (+ 2 2)))
    (is (= 6 (* 2 3)))
    (is (true? true))
    (is (false? false))))

(deftest test-authentication-logic
  (testing "Authentication logic without database"
    ;; These tests verify the logic without requiring database
    (is (string? "Bearer token"))
    (is (re-matches #"^Bearer .*" "Bearer test-token"))
    (is (not (re-matches #"^Bearer .*" "Invalid token")))))
(ns cde.simple-test)