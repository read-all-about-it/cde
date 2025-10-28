(ns cde.views.settings
  "User settings page for authenticated users.

  Displays user account information and provides logout functionality.
  Redirects unauthenticated users to the home page."
  (:require
   [re-frame.core :as rf]
   [cde.events]
   [cde.subs]
   [cde.components.login :as login]))

(defn settings-page
  "Renders the user settings page.

  Shows the logged-in user's email and a logout button. If the user
  is not authenticated, returns nil (the route controller handles
  redirecting unauthenticated users to the home page)."
  []
  (let [logged-in? @(rf/subscribe [:auth/logged-in?])
        username @(rf/subscribe [:auth/user-email])]
    (when logged-in?
      [:section.section>div.container>div.content
       [:h1
        {:style {:text-align "center"}}
        "User Settings"]
       [:p "Hi " [:strong username] "!"]
       [:p "This is your settings page."]
       [login/auth0-logout-button]])))
