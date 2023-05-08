(ns cde.components.nav 
  (:require 
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.components.login :as login]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [burger-expanded? (r/atom false)
               user (rf/subscribe [:auth/user])]
    [:nav.navbar.is-info
     [:div.container
      [:div.navbar-brand
       [:a.navbar-item {:href "/" :style {:font-weight :bold}} "TBC"]
       [:span.navbar-burger.burger
        {:data-target :nav-menu
         :on-click #(swap! burger-expanded? not)
         :class (when @burger-expanded? :is-active)}
        [:span] [:span] [:span]]]
      [:div#nav-menu.navbar-menu
       {:class (when @burger-expanded? :is-active)}
       (if-some [user @(rf/subscribe [:auth/user])]
         [:div.navbar-start
          [nav-link "#/about" "About" :about]
          [nav-link "#/search" "Explore" :search]
          [nav-link "#/contribute" "Contribute" :contribute]]
         [:div.navbar-start
          [nav-link "#/about" "About" :about]
          [nav-link "#/search" "Explore" :search]])
       [:div.navbar-end
        [:div.navbar-item
         (if-some [user @(rf/subscribe [:auth/user])]
           [:div.buttons
            [login/nameplate user]
            [login/logout-button]]
           [:div.buttons
            [login/register-button]
            [login/login-button]])]]]]]))
