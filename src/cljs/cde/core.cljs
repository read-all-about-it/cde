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
   [cde.pages.create :refer [add-a-newspaper-page]]
   [cde.pages.profile :refer [profile-page]]
   [cde.pages.newspaper :refer [newspaper-page]]
   [cde.pages.author :refer [author-page]]
   [cde.pages.chapter :refer [chapter-page]]
   [cde.pages.title :refer [title-page]])
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
          :view        #'home-page
          :controllers [{:start (fn [_]
                                  (rf/dispatch [:fetch-landing-page-text])
                                  (rf/dispatch [:platform/get-statistics]))}]}]
    ["/about" {:name :about
               :view #'about-page}]
    ["/search" {:name :search
                :view #'search-page
                :controllers [{:start (fn [_] (rf/dispatch [:platform/get-search-options]))
                               :stop (fn [_] (rf/dispatch [:search/clear-search-query]))}]}]
    ["/contribute" {:name :contribute
                    :view #'contribute-page}]
    ["/settings" {:name :settings
                  :view #'settings-page}]
    ["/add/newspaper" {:name :add-newspaper
                       :view #'add-a-newspaper-page}]
    ["/profile/:id" {:name :public-profile
                     :view #'profile-page
                     :controllers [{:start (fn [_] (rf/dispatch [:profile/request-profile]))
                                    :stop (fn [_] (rf/dispatch [:profile/clear-profile]))}]}]
    ["/newspaper/:id" {:name :newspaper-page
                       :view #'newspaper-page
                       :controllers [{:start (fn [_] (rf/dispatch [:newspaper/request-newspaper-metadata]))
                                      :stop (fn [_] (rf/dispatch [:newspaper/clear-newspaper]))}]}]
    ["/author/:id" {:name :author-page
                    :view #'author-page
                    :controllers [{:start (fn [_] (rf/dispatch [:author/request-author-metadata]))
                                   :stop (fn [_] (rf/dispatch [:author/clear-author]))}]}]
    ["/title/:id" {:name :title-page
                   :view #'title-page
                   :controllers [{:start (fn [_] (rf/dispatch [:title/request-title]))
                                  :stop (fn [_] (rf/dispatch [:title/clear-title]))}]}]
    ["/chapter/:id" {:name :chapter-page
                     :view #'chapter-page
                     :controllers [{:start (fn [_] (rf/dispatch [:chapter/request-chapter]))
                                    :stop (fn [_] (rf/dispatch [:chapter/clear-chapter]))}]}]]))

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
