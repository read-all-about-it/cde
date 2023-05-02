(ns cde.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [cde.ajax :as ajax]
   [cde.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [cde.components.modals :as modals]
   [cde.components.nav :as nav])
  (:import goog.History))

(defn about-page []
  [:section.section>div.container>div.content
   [:h1 "About"]
   [:p "Test text."]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

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
                :view #'about-page}]]))

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
