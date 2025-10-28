(ns cde.auth-test
  (:require
   [cljs.test :refer-macros [deftest testing is async]]
   [re-frame.core :as rf]
   [re-frame.db :as rf-db]
   [day8.re-frame.test :as rf-test]
   [cde.events :as events]
   [cde.subs :as subs]))

;; Test helpers
(defn setup-test-db
  "Initialize test database"
  []
  (rf/dispatch-sync [:initialize-db])
  (rf-test/clear-subscription-cache!))

(defn mock-auth0-client
  "Create a mock Auth0 client for testing"
  []
  (clj->js
   {:loginWithPopup (fn [_]
                      (js/Promise.resolve))
    :logout (fn []
              (js/Promise.resolve))
    :getUser (fn []
               (js/Promise.resolve
                (clj->js {:sub "auth0|test123"
                          :email "test@example.com"
                          :name "Test User"
                          :email_verified true})))
    :getTokenSilently (fn [_]
                        (js/Promise.resolve "mock-jwt-token"))
    :isAuthenticated (fn []
                       (js/Promise.resolve false))
    :handleRedirectCallback (fn []
                              (js/Promise.resolve))}))

;; Tests for auth events

(deftest test-auth-initialization
  (testing "Auth initialization creates Auth0 client"
    (rf-test/run-test-sync
     (setup-test-db)
     (rf/dispatch [:auth/initialise])
     (is (some? @(rf/subscribe [:auth/auth0-client]))))))

(deftest test-auth-store-user
  (testing "Storing Auth0 user in database"
    (rf-test/run-test-sync
     (setup-test-db)
     (let [test-user {:sub "auth0|123"
                      :email "test@example.com"
                      :name "Test User"}]
       (rf/dispatch [:auth/store-auth0-user-in-db test-user])
       (let [stored-user @(rf/subscribe [:auth/user])]
         (is (= (:email stored-user) "test@example.com"))
         (is (= (:sub stored-user) "auth0|123")))))))

(deftest test-auth-store-tokens
  (testing "Storing Auth0 tokens in database"
    (rf-test/run-test-sync
     (setup-test-db)
     (rf/dispatch [:auth/store-auth0-tokens-in-db "test-jwt-token"])
     (let [stored-token @(rf/subscribe [:auth/tokens])]
       (is (= stored-token "test-jwt-token"))))))

(deftest test-auth-logged-in-status
  (testing "User logged in status"
    (rf-test/run-test-sync
     (setup-test-db)
     ;; Initially not logged in
     (is (false? @(rf/subscribe [:auth/logged-in?])))
     ;; Store user
     (rf/dispatch [:auth/store-auth0-user-in-db {:email "test@example.com"}])
     ;; Now logged in
     (is (true? @(rf/subscribe [:auth/logged-in?]))))))

(deftest test-auth-logout
  (testing "User logout clears auth state"
    (rf-test/run-test-sync
     (setup-test-db)
     ;; Setup logged in state
     (rf/dispatch [:auth/store-auth0-user-in-db {:email "test@example.com"}])
     (rf/dispatch [:auth/store-auth0-tokens-in-db "test-token"])
     (is (true? @(rf/subscribe [:auth/logged-in?])))
     ;; Logout
     (rf/dispatch [:auth/remove-auth0-user-from-db])
     ;; Check state is cleared
     (is (false? @(rf/subscribe [:auth/logged-in?])))
     (is (nil? @(rf/subscribe [:auth/tokens]))))))

(deftest test-auth-header-generation
  (testing "Auth header generation from token"
    (rf-test/run-test-sync
     (setup-test-db)
     (rf/dispatch [:auth/store-auth0-tokens-in-db "test-jwt-token"])
     (let [db @rf-db/app-db
           header (events/auth-header db)]
       (is (map? header))
       (is (= (get header "Authorization") "Bearer test-jwt-token"))))))

(deftest test-auth-header-without-token
  (testing "Auth header returns nil without token"
    (rf-test/run-test-sync
     (setup-test-db)
     (let [db @rf-db/app-db
           header (events/auth-header db)]
       (is (nil? header))))))

;; Tests for auth subscriptions

(deftest test-auth-subscriptions
  (testing "Auth subscriptions return correct values"
    (rf-test/run-test-sync
     (setup-test-db)
     (let [test-user {:sub "auth0|123"
                      :email "test@example.com"
                      :nickname "testuser"
                      :email_verified true}]
       (rf/dispatch [:auth/store-auth0-user-in-db test-user])
       (rf/dispatch [:auth/store-auth0-tokens-in-db "test-token"])

       ;; Test all auth subscriptions
       (is (= @(rf/subscribe [:auth/user-email]) "test@example.com"))
       (is (= @(rf/subscribe [:auth/user-nickname]) "testuser"))
       (is (true? @(rf/subscribe [:auth/user-email-verified?])))
       (is (= @(rf/subscribe [:auth/tokens]) "test-token"))
       (is (true? @(rf/subscribe [:auth/logged-in?])))))))

;; Tests for localStorage integration

(deftest test-local-storage-persistence
  (testing "Auth state persists to localStorage"
    (rf-test/run-test-sync
     (setup-test-db)
     ;; Mock localStorage
     (let [storage (atom {})]
       (set! (.-localStorage js/window)
             #js {:setItem (fn [k v] (swap! storage assoc k v))
                  :getItem (fn [k] (get @storage k))
                  :removeItem (fn [k] (swap! storage dissoc k))})

       ;; Store auth data
       (rf/dispatch [:auth/set-auth-in-ls [{:auth {:user {:email "test@example.com"}}}]])

       ;; Check localStorage was updated
       (is (some? (get @storage "to-be-continued-user")))))))

;; Async tests for promises

(deftest test-auth0-login-flow
  (testing "Auth0 login flow with mock client"
    (async done
           (rf-test/run-test-async
            (setup-test-db)
       ;; Set mock Auth0 client
            (rf/dispatch-sync [:auth/create-auth0-client])
            (swap! rf-db/app-db assoc :auth0-client (mock-auth0-client))

       ;; Trigger login
            (rf/dispatch [:auth/login-auth0-with-popup])

       ;; Wait for async operations
            (js/setTimeout
             (fn []
               (let [user @(rf/subscribe [:auth/user])]
                 (is (some? user))
                 (is (= (:email user) "test@example.com"))
                 (done)))
             100)))))

(deftest test-auth0-token-fetch
  (testing "Auth0 token fetching"
    (async done
           (rf-test/run-test-async
            (setup-test-db)
       ;; Set mock Auth0 client
            (swap! rf-db/app-db assoc :auth0-client (mock-auth0-client))

       ;; Trigger token fetch
            (rf/dispatch [:auth/get-auth0-tokens])

       ;; Wait for async operations
            (js/setTimeout
             (fn []
               (let [token @(rf/subscribe [:auth/tokens])]
                 (is (= token "mock-jwt-token"))
                 (done)))
             100)))))
