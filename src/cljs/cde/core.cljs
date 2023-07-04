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
   [cde.pages.about :refer [about-page faq-page team-page]]
   [cde.pages.search :refer [search-page]]
   [cde.pages.contribute :refer [contribute-page]]
   [cde.pages.settings :refer [settings-page]]
   [cde.pages.profile :refer [profile-page]]
   [cde.pages.newspaper :refer [newspaper-page
                                create-a-newspaper]]
   [cde.pages.author :refer [author-page
                             edit-an-author]]
   [cde.pages.chapter :refer [chapter-page
                              create-a-chapter]]
   [cde.pages.title :refer [title-page
                            create-a-title
                            edit-a-title]]
   [cde.pages.test :refer [test-page]])
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
                                  (rf/dispatch [:platform/get-statistics]))}]}]

    ["/test" {:name :test
              :view #'test-page}]

    ["/about" {:name :about
               :view #'about-page
               :controllers [{:start (fn [_] (rf/dispatch [:platform/fetch-about-txt]))}]}]
    ["/faq" {:name :faq
             :view #'faq-page
             :controllers [{:start (fn [_] (rf/dispatch [:platform/fetch-faq-txt]))}]}]
    ["/team" {:name :team
              :view #'team-page
              :controllers [{:start (fn [_] (rf/dispatch [:platform/fetch-team-txt]))}]}]

    ["/search" {:name :search
                :view #'search-page
                :controllers [{:start (fn [_] (rf/dispatch [:platform/get-search-options]))
                               :stop (fn [_] (rf/dispatch [:search/clear-search-query]))}]}]

    ["/contribute" {:name :contribute
                    :view #'contribute-page}]

    ;; ["/settings" {:name :settings
    ;;               :view #'settings-page}]
    ;; ["/profile/:id" {:name :public-profile
    ;;                  :view #'profile-page
    ;;                  :controllers [{:start (fn [_] (rf/dispatch [:profile/request-profile]))
    ;;                                 :stop (fn [_] (rf/dispatch [:profile/clear-profile]))}]}]

    ;; NEWSPAPER ROUTES
    ;; ["/add/newspaper" {:name :add-newspaper
    ;;                    :view #'create-a-newspaper}]}]
    ["/newspaper/:id" {:name :newspaper-page
                       :view #'newspaper-page
                       :controllers [{:start (fn [_] (rf/dispatch [:newspaper/get-newspaper]))
                                      :stop (fn [_] (rf/dispatch [:newspaper/clear-newspaper]))}]}]
    ;; ["/edit/newspaper/:id" {:name :edit-newspaper
    ;;                         :view #'edit-a-newspaper
    ;;                         :controllers [{:start (fn [_] (rf/dispatch [:newspaper/get-newspaper]))
    ;;                                        :stop (fn [_] (rf/dispatch [:newspaper/clear-edit-newspaper-form]))}]

    ;; AUTHOR ROUTES
    ;; ["/add/author" {:name :add-author :view #'add-author
    ;;                 :view #'create-an-author}]
    ["/author/:id" {:name :author-page
                    :view #'author-page
                    :controllers [{:start (fn [_] (rf/dispatch [:author/get-author]))
                                   :stop (fn [_] (rf/dispatch [:author/clear-author]))}]}]
    ["/edit/author/:id" {:name :edit-author
                         :view #'edit-an-author
                         :controllers [{:start (fn [_] (rf/dispatch [:author/get-author]))
                                        :stop (fn [_] (rf/dispatch [:author/clear-edit-author-form]))}]}]

    ;; TITLE ROUTES
    ["/add/title" {:name :add-title
                   :view #'create-a-title
                   :controllers [{:start (fn [_] (rf/dispatch [:title/prepop-new-title-form-from-query-params]))
                                  ;; :stop (fn [_] (rf/dispatch [:title/clear-new-title-form]))
                                  }]}]

    ["/title/:id" {:name :title-page
                   :view #'title-page
                   :controllers [{:start (fn [_] (rf/dispatch [:title/get-title]))
                                  :stop (fn [_] (rf/dispatch [:title/clear-title]))}]}]
    ["/edit/title/:id" {:name :edit-title
                        :view #'edit-a-title
                        :controllers [{:start (fn [_] (rf/dispatch [:title/get-title]))
                                       :stop (fn [_] (rf/dispatch [:title/clear-edit-title-form]))}]}]

    ;; CHAPTER ROUTES
    ["/add/chapter" {:name :add-chapter
                     :view #'create-a-chapter
                     :controllers [{:start (fn [_] (rf/dispatch [:chapter/prepop-new-chapter-form-from-query-params]))
                                    :stop (fn [_] (rf/dispatch [:chapter/clear-new-chapter-form]))}]}]
    ["/chapter/:id" {:name :chapter-page
                     :view #'chapter-page
                     :controllers [{:start (fn [_] (rf/dispatch [:chapter/get-chapter]))
                                    :stop (fn [_] (rf/dispatch [:chapter/clear-chapter]))}]}]
    ;; ["/edit/chapter/:id" {:name :edit-chapter
    ;;                       :view #'edit-a-chapter
    ;;                       :controllers [{:start (fn [_] (rf/dispatch [:chapter/get-chapter]))
    ;;                                      :stop (fn [_] (rf/dispatch [:chapter/clear-edit-chapter-form]))}]}]
    ]))

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
  (rf/dispatch [:auth/initialise])
  (mount-components))