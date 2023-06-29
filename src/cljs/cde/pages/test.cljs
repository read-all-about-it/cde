(ns cde.pages.test
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]
   [cde.components.nav :refer [page-header]]
   ))

(defn auth0-status-component []
  (let [auth0-client (rf/subscribe [:auth0-client])]
    (fn []
      (if @auth0-client
        [:div "Auth0 client initialized"]
        [:div "Auth0 client not initialized"]))))

(defn test-login-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:login])}
    "Login"]])

(defn test-page []
  (fn []
    [:section.section>div.container>div.content
     [page-header "Test"]
     [auth0-status-component]
     [test-login-button]]))