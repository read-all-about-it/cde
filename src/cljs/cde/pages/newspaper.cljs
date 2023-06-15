(ns cde.pages.newspaper
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table titles-table]]
   [cde.components.nav :refer [record-buttons]]
   [cde.utils :refer [details->metadata]]))


(defn newspaper-page
  []
  (r/with-let [metadata-loading? (rf/subscribe [:newspaper/metadata-loading?])
               titles-loading? (rf/subscribe [:newspaper/titles-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               newspaper-metadata (rf/subscribe [:newspaper/details])
               titles-in-newspaper (rf/subscribe [:newspaper/titles])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       [:div
        (when-not @metadata-loading?
          [:h1 {:style {:text-align "center"}} (:common_title @newspaper-metadata)])
        (when-not @metadata-loading?
          [:h3 {:style {:text-align "center"}} "Newspaper Metadata"])
        (when-not @metadata-loading?
          [metadata-table (details->metadata @newspaper-metadata :newspaper)])
        (when-not @titles-loading?
          (if-not (empty? @titles-in-newspaper)
            [:div
             [:h3 {:style {:text-align "center"}} "Titles in Newspaper"]
             [titles-table @titles-in-newspaper :newspaper]]
            [:h3 {:style {:text-align "center"}} "No titles found in this Newspaper"]))
        (when (empty? @titles-in-newspaper)
          [:div
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:newspaper/request-titles-in-newspaper])}
            "View Titles"]])]])))

(defn create-a-newspaper
  []
  (r/with-let [details (rf/subscribe [:newspaper/new-newspaper-form])
               error (r/atom nil)]
    [:section.section>div.container>div.content
     [:div
      [:h1 {:style {:text-align "center"}} "Add A Newspaper"]
      ;; TODO: ADD FORM FIELDS
      ]]
    ))