(ns cde.pages.title
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.forms :refer [new-title-form]]
   [cde.components.metadata :refer [metadata-table basic-chapter-table chapter-table]]
   [cde.utils :refer [details->metadata
                      records->table-data]]))


(defn title-page
  []
  (r/with-let [metadata-loading? (rf/subscribe [:title/metadata-loading?])
               chapters-loading? (rf/subscribe [:title/chapters-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               title-metadata (rf/subscribe [:title/details])
               chapters-in-title (rf/subscribe [:title/chapters])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       [:div
        (when-not @metadata-loading?
          [:h1 {:style {:text-align "center"}} (:common_title @title-metadata)])
        (when-not @metadata-loading?
          [:h3 {:style {:text-align "center"}} "Title Metadata"])
        (when-not @metadata-loading?
          [metadata-table (details->metadata @title-metadata :title)])
        (when-not @chapters-loading?
          (if-not (empty? @chapters-in-title)
            [:div
             [:h3 {:style {:text-align "center"}} "Discovered Chapters"]
             [basic-chapter-table @chapters-in-title]
             [chapter-table (records->table-data @chapters-in-title :chapter)]]
            [:div [:h3 {:style {:text-align "center"}} "No Chapters Found in this title record!"]]))
        (when (empty? @chapters-in-title)
          [:div
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:title/request-chapters-in-title])}
            "View Chapters"]])]])))


(defn create-a-title ;; form for creating a new title
  []
  (fn []
    [:section.section>div.container>div.content
     [:div
      [:h1 {:style {:text-align "center"}} "Add A Title"]
      [new-title-form]
      ]]))