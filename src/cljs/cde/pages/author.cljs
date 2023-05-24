(ns cde.pages.author
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [simple-metadata-block metadata-table]]))


(defn author-page
  []
  (r/with-let [loading? (rf/subscribe [:author/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               author (rf/subscribe [:author/details])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:div
          [:h1 {:style {:text-align "center"}} (:common_name @author)]
          [:h3 {:style {:text-align "center"}} "(Author Details)"]
          (when @logged-in?
            [:div])
          [simple-metadata-block @author
           [:common_name :other_name :nationality :nationality_details :author_details]
           {:common_name "Common Name"
            :other_name "Other Name(s)"
            :nationality "Nationality"
            :nationality_details "Nationality Details"
            :author_details "Source of Author Details"}]])])))