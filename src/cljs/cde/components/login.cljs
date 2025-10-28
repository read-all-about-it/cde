(ns cde.components.login
  "Authentication UI components for Auth0 integration.

  Provides login/logout buttons and user display components that
  integrate with the Auth0 authentication flow via re-frame events."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.events]
   [cde.subs]))

(defn auth0-login-button
  "Primary login button that triggers Auth0 popup authentication."
  []
  [:div
   [:button.button.is-primary
    {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])}
    "Login"]])

(defn auth0-login-to-edit-button
  "Centered login button with 'Login to Edit' text for edit pages."
  []
  [:div.block.has-text-centered
   [:button.button.is-primary
    {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])}
    "Login to Edit"]])

(defn auth0-logout-button
  "Logout button that triggers Auth0 logout flow."
  []
  [:div
   [:button.button
    {:on-click #(rf/dispatch [:auth/logout-auth0])}
    "Logout"]])

(defn nameplate
  "Displays the logged-in user's email in a button-styled nameplate.

  Arguments:
  - `user` - map with `:email` key (from Auth0 user profile)"
  [{:keys [email]}]
  [:button.button.is-primary
   [:span.icon
    [:i.material-icons "person"]]
   [:span
    (if email
      (str " " email)
      " User")]])
