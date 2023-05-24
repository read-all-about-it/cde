(ns cde.pages.title
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table simple-metadata-block chapter-table]]))


;;(defn- convert-title-details-to-metadata
;;  "Take the details of a title and convert it to a map of metadata suitable for the 'metadata-table' component."
;;  []
;;  nil)

(defn title-page
  []
  (r/with-let [loading? (rf/subscribe [:title/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               title (rf/subscribe [:title/details])
               chapters-in-title (rf/subscribe [:title/chapters])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:div
          [:h1 {:style {:text-align "center"}} (:common_title @title)]
          [:h3 {:style {:text-align "center"}} "(Title Details)"]
          (when @logged-in?
            [:div])
          [simple-metadata-block @title
           [:publication_title
            :common_title
            :span_start
            :span_end
            :name_category]
           {:publication_title "Publication Title"
            :common_title "Common Title"
            :span_start "Start Date"
            :span_end "End Date"
            :name_category "Name Category"}]
          (if-not (empty? @chapters-in-title)
            [:div
             [:h3 {:style {:text-align "center"}} "Chapters"]
             [chapter-table @chapters-in-title]]
            [:div
             [:button.button.is-primary
              {:on-click #(rf/dispatch [:title/request-chapters-in-title])}
              "View Chapters"]])])])))