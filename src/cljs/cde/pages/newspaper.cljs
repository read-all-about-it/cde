(ns cde.pages.newspaper
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table]]
   [cde.components.nav :refer [record-buttons]]
   [cde.utils :refer [details->metadata]]))


(defn newspaper-page
  []
  (r/with-let [loading? (rf/subscribe [:newspaper/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               newspaper (rf/subscribe [:newspaper/details])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:div
          [:h1 {:style {:text-align "center"}} (:common_title @newspaper)]
          [:h3 {:style {:text-align "center"}} "Newspaper Metadata"]
          (when @logged-in?
            [:div])
          (when @newspaper
            [metadata-table (details->metadata @newspaper :newspaper)])])])))