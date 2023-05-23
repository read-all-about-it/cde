(ns cde.pages.newspaper
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [simple-metadata-block]]))


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
          [:h3 {:style {:text-align "center"}} "(Newspaper Details)"]
          (when @logged-in?
            [:div])
          [simple-metadata-block @newspaper
           [:title :common_title :location :start_date :end_date :colony_state :details]
           {:title "Title" :common_title "Common Title" :location "Location" :start_date "Start Date" :end_date "End Date" :colony_state "Colony/State" :details "Details"}]])])))