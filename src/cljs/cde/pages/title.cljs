(ns cde.pages.title
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-block]]))


(defn title-page
  []
  (r/with-let [loading? (rf/subscribe [:title/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               title (rf/subscribe [:title/details])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:div
          [:h1 {:style {:text-align "center"}} (:common_title @title)]
          [:h3 {:style {:text-align "center"}} "(Title Details)"]
          (when @logged-in?
            [:div])
          [metadata-block @title
           [:publication_title
            :common_title
            :span_start
            :span_end
            :name_category]
           {:publication_title "Publication Title"
            :common_title "Common Title"
            :span_start "Start Date"
            :span_end "End Date"
            :name_category "Name Category"}
           ]])])))