(ns cde.pages.settings
  (:require
   [re-frame.core :as rf]
   [cde.events]
   [cde.subs]
   [cde.components.login :as login]))


(defn settings-page []
  (let [logged-in? @(rf/subscribe [:auth/logged-in?])
        username @(rf/subscribe [:auth/user-email])]
    (if logged-in?
      [:section.section>div.container>div.content
       [:h1
        {:style {:text-align "center"}}
        "User Settings"] 
       [:p "Hi " [:strong username] "!"]
       [:p "This is your settings page."]
       [login/auth0-logout-button]]
      ;; if not logged in, bump to home page
      (rf/dispatch [:common/navigate! :home]))))