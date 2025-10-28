(ns cde.core
  "SPA entry point and client-side routing configuration.

  This namespace initialises the ClojureScript application:
  - Sets up Reitit client-side routing with view components
  - Configures route controllers for data fetching on navigation
  - Mounts the root Reagent component to the DOM
  - Initialises Auth0 authentication

  Key components:
  - `router`: Reitit router with all application routes
  - `page`: Root component that renders navbar + current view
  - `init!`: Application initialisation function

  See also: [[cde.events]], [[cde.subs]] for state management."
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
   [cde.views.home :refer [home-page]]
   [cde.views.about :refer [about-page faq-page team-page]]
   [cde.views.search :refer [search-page]]
   [cde.views.contribute :refer [contribute-page]]
   [cde.views.newspaper :refer [newspaper-page
                                create-a-newspaper
                                edit-a-newspaper]]
   [cde.views.author :refer [author-page
                             edit-an-author
                             create-an-author]]
   [cde.views.chapter :refer [chapter-page
                              create-a-chapter
                              edit-a-chapter]]
   [cde.views.title :refer [title-page
                            create-a-title
                            edit-a-title]]
   [cde.views.settings :refer [settings-page]])
  (:import goog.History))

(defn page
  "Root component that renders the navigation bar and current page view.

  Subscribes to `:common/page` to get the current view component based
  on the active route. Returns nil if no route is matched."
  []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [nav/navbar]
     [page]]))

(defn navigate!
  "Navigation callback invoked by Reitit on route changes.

  Dispatches the `:common/navigate` event with the matched route data,
  which triggers controller lifecycle hooks and updates the app state."
  [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  "Reitit router containing all client-side routes.

  Each route defines:
  - `:name` - Unique route identifier for navigation
  - `:view` - Reagent component to render
  - `:controllers` - Lifecycle hooks for data fetching/cleanup

  Controllers have `:start` (on enter) and `:stop` (on leave) functions
  that dispatch re-frame events for loading and clearing data."
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_]
                                  (rf/dispatch [:platform/get-statistics]))}]}]

    ["/about" {:name :about
               :view #'about-page
               :controllers [{:start (fn [_] (rf/dispatch [:platform/fetch-about-txt]))}]}]
    ;; ["/faq" {:name :faq
    ;;          :view #'faq-page
    ;;          :controllers [{:start (fn [_] (rf/dispatch [:platform/fetch-faq-txt]))}]}]
    ["/team" {:name :team
              :view #'team-page
              :controllers [{:start (fn [_] (rf/dispatch [:platform/fetch-team-txt]))}]}]

    ["/search" {:name :search
                :view #'search-page
                :controllers [{:start (fn [_] (rf/dispatch [:platform/get-search-options]))
                               :stop (fn [_] (rf/dispatch [:search/clear-search-query]))}]}]

    ["/contribute" {:name :contribute
                    :view #'contribute-page}]

    ["/login" {:name :login
               :view #'contribute-page
               :controllers [{:start (fn [_] (rf/dispatch [:auth/login-auth0-with-popup]))}]}]

    ["/settings" {:name :settings
                  :view #'settings-page
                  :controllers [{:start (fn [_] (rf/dispatch [:common/require-auth]))}]}]

    ;; NEWSPAPER ROUTES
    ["/add/newspaper" {:name :add-newspaper
                       :view #'create-a-newspaper
                       :controllers [{:stop (fn [_] (rf/dispatch [:newspaper/clear-new-newspaper-form]))}]}]
    ["/newspaper/:id" {:name :newspaper-page
                       :view #'newspaper-page
                       :controllers [{:start (fn [_] (rf/dispatch [:newspaper/get-newspaper]))
                                      :stop (fn [_] (rf/dispatch [:newspaper/clear-newspaper]))}]}]
    ["/edit/newspaper/:id" {:name :edit-newspaper
                            :view #'edit-a-newspaper
                            :controllers [{:start (fn [_] (rf/dispatch [:newspaper/get-newspaper]))
                                           :stop (fn [_] (rf/dispatch [:newspaper/clear-edit-newspaper-form]))}]}]

    ;; AUTHOR ROUTES
    ["/add/author" {:name :add-author
                    :view #'create-an-author
                    :controllers [{:stop (fn [_] (rf/dispatch [:author/clear-new-author-form]))}]}]
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
                                  :stop (fn [_] (rf/dispatch [:title/clear-new-title-form]))}]}]

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
    ["/edit/chapter/:id" {:name :edit-chapter
                          :view #'edit-a-chapter
                          :controllers [{:start (fn [_] (rf/dispatch [:chapter/get-chapter]))
                                         :stop (fn [_] (rf/dispatch [:chapter/clear-edit-chapter-form]))}]}]]))

(defn start-router!
  "Initialises the Reitit frontend router with HTML5 history navigation.

  Connects the `router` to browser history events and sets `navigate!`
  as the callback for route changes."
  []
  (rfe/start!
   router
   navigate!
   {}))

;;;; Application Initialisation

(defn ^:dev/after-load mount-components
  "Mounts the root Reagent component to the DOM.

  Called on initial load and after hot-reloads in development.
  Clears re-frame subscription cache to ensure fresh state."
  []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init!
  "Initialises the application on page load.

  Performs startup sequence:
  1. Starts the client-side router
  2. Loads AJAX interceptors (auth headers)
  3. Initialises authentication from localStorage
  4. Mounts React components to the DOM"
  []
  (start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch [:auth/initialise])
  (mount-components))
