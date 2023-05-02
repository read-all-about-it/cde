(ns cde.components.nav 
  (:require 
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.components.modals :as modals]))


(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "cde"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]]
      [:div.navbar-end
       [:div.navbar-item
        (if-some [user @(rf/subscribe [:auth/user])]
          [:div.buttons
           [modals/nameplate user]
           [modals/logout-button]]
          [:div.buttons
           [modals/register-button]
           [modals/login-button]])]]]]))
