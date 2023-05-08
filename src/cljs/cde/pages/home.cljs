(ns cde.pages.home
  (:require 
   [re-frame.core :as rf] 
   [cde.events]
   [cde.subs]
   [markdown.core :refer [md->html]]
   [cde.components.login :as login]
   ))

;; THE 'HOME' PAGE ('/')
;; The introduction/root page of the application, with a brief description of
;; the project, highlights, a link to sign-up, and a link to explore via the
;; search page.

(defn home-page []
  [:section.section>div.container>div.content
   [:h1 {:style {:text-align "center"}}
    "Welcome to the 'To Be Continued' project!"] 
   [:p "This dummy text will be replaced with a nice, fun launch page encouraging users to join the platform. And maybe some enticing highlights of recent projects?"]
   ;; an example of bulma 3 column tiles using div-tile
   [:div.tile.is-ancestor
    [:div.tile.is-parent.is-vertical
     [:article.tile.is-child.notification.is-primary
      [:p.subtitle "A Recent Additon"]
      [:p "Some text!"]]]
    [:div.tile.is-parent.is-vertical
     [:article.tile.is-child.notification.is-warning
      [:p.subtitle "Another!"]
      [:p "Some text!"]]]
    [:div.tile.is-parent.is-vertical
     [:article.tile.is-child.notification.is-success
      [:p.subtitle "A third!"]
      [:p "Some text!"]]]]
   
   (when-let [home-page-dummy-text @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html home-page-dummy-text)}}])
   [:br]
   ;; place the 'login/register' button in the center of the page
   [:div.container {:style {:text-align "center"}}
    [login/register-button]]
   ])