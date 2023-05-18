(ns cde.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [cde.ajax :as ajax]
   [cde.events]
   [cde.subs]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [cde.components.nav :as nav]
   [cde.pages.home :refer [home-page]]
   [cde.pages.about :refer [about-page]]
   [cde.pages.search :refer [search-page]]
   [cde.pages.contribute :refer [contribute-page]]
   [cde.pages.settings :refer [settings-page]]
   [cde.pages.add :refer [add-a-newspaper-page]]
   [cde.pages.profile :refer [profile-page]])
  (:import goog.History))



(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [nav/navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page}]
     ["/about" {:name :about
                :view #'about-page}]
     ["/search" {:name :search
                 :view #'search-page}]
     ["/contribute" {:name :contribute
                     :view #'contribute-page}]
     ["/settings" {:name :settings
                   :view #'settings-page}]
     ["/add/newspaper" {:name :add-newspaper
                        :view #'add-a-newspaper-page}]
     ;; ["/add/title" {:name :add-title
     ;;               :view #'add-title-page}]
     ;; ["/add/chapter" {:name :add-chapter
     ;;                 :view #'add-chapter-page}]
     ["/profile/:id" {:name :profile
                      :view #'profile-page
                      :controllers [{:start (fn [_] (rf/dispatch [:profile/request-profile]))
                                     :stop (fn [_] (rf/dispatch [:profile/clear-profile]))}]}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting components...")
  (rdom/render [#'page] (.getElementById js/document "app"))
  (.log js/console "Components mounted."))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
