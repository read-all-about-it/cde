(ns cde.pages.test
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.subs]
   [cde.events]
   [cde.components.nav :refer [page-header]]
   ))

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

(defn test-page []
  (fn []
    [:section.section>div.container>div.content
     [page-header "Test"]
     [auth0-status-component]
     [:br]
     [print-auth0-client-to-console-button]
     [:br]
     [login-auth0-with-popup-button]]))


(defn callback-view []
  (fn []
    [:section.section>div.container>div.content
     [page-header "Callback"]
     [auth0-status-component]
     [:div "Callback"]]))