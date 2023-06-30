(ns cde.components.login
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.events]
   [cde.subs]))

(defn auth0-login-button
  []
  [:div
   [:button.button.is-primary
    {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])}
    "Login"]])

(defn auth0-logout-button
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/logout-auth0])}
    "Logout"]])


(defn nameplate [{:keys [email]}]
  [:button.button.is-primary
   [:span.icon
    [:i.material-icons "person"]]
   [:span
    (if email
      (str " " email)
      " User")]])