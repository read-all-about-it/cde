(ns cde.views.test
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]
   [cde.components.nav :refer [page-header]]
   ))


(defn create-auth0-client-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/create-auth0-client])}
    "Create Auth0 Client"]])

(defn auth0-status-component []
  (let [auth0-client (rf/subscribe [:auth/auth0-client])]
    (fn []
      (if @auth0-client
        [:div "Auth0 client initialized"]
        [:div "Auth0 client not initialized"]))))

(defn print-auth0-client-to-console-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/print-auth0-client])}
    "Print Auth0 Client to Console"]])

(defn login-auth0-with-popup-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])}
    "Login Auth0 with Popup"]])

(defn logout-auth0-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/logout-auth0])}
    "Logout Auth0"]])

(defn get-auth0-tokens-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/get-auth0-tokens])}
    "Get Auth0 Token (Silent)"]])

(defn test-auth-endpoint-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/test-auth])}
    "Test Auth Endpoint"]])

(defn test-auth-endpoint-without-auth-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/test-auth-without-auth])}
    "Test Auth Endpoint Without Auth"]])

(defn test-page []
  (fn []
    [:section.section>div.container>div.content
     [page-header "Test"]
     [auth0-status-component]
     [:br]
     [create-auth0-client-button]
     [:br]
     [print-auth0-client-to-console-button]
     [:br]
     [login-auth0-with-popup-button]
     [:br]
     [logout-auth0-button]
     [:br]
     [get-auth0-tokens-button]
     [:br]
     [test-auth-endpoint-button]
     [:br]
     [test-auth-endpoint-without-auth-button]
     [:br]]))


(defn callback-view []
  (fn []
    [:section.section>div.container>div.content
     [page-header "Callback"]
     [auth0-status-component]
     [:div "Callback"]]))