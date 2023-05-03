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
   [cde.pages.search :refer [search-page]])
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
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]
     ["/search" {:name :search
                 :view #'search-page}]]))

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
