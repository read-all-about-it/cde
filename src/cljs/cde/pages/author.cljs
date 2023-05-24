(ns cde.pages.author
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table titles-table]]
   [cde.utils :refer [details->metadata]]))

(defn- convert-author-details-to-metadata
  "Takes a title details map and converts it to a vector of maps suitable for the 'metadata-table' component."
  [result]
  (->>
   [{:title "Author Name"
     :value (:common_name result)}
    {:title "Other Attributed Names"
     :value (:other_name result)
     :keep true}
    {:title "Gender"
     :value (:gender result)}
    {:title "Author Nationality"
     :value (:nationality result)}
    {:title "Nationality Details"
     :value (:nationality_details result)}
    {:title "Source of Author Details"
     :value (:author_details result)}]
   ; remove all results where the value is nil and 'keep' is not true
   (filter #(and (not (nil? (:value %))) (not= true (:keep %))))))

(defn author-page
  []
  (r/with-let [metadata-loading? (rf/subscribe [:author/metadata-loading?])
               titles-loading? (rf/subscribe [:author/titles-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               author-metadata (rf/subscribe [:author/details])
               titles-by-author (rf/subscribe [:author/titles])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       [:div
        (when-not @metadata-loading?
          [:h1 {:style {:text-align "center"}} (:common_name @author-metadata)])
        (when-not @metadata-loading?
          [:h3 {:style {:text-align "center"}} "Author Metadata"])
        (when-not @metadata-loading?
          [metadata-table (convert-author-details-to-metadata @author-metadata)])
        (when-not @titles-loading?
          (if-not (empty? @titles-by-author)
            [:div
             [:h3 {:style {:text-align "center"}} "Attributed Titles"]
             [titles-table @titles-by-author]]
            [:h3 {:style {:text-align "center"}} "No titles found for this author"]))
        (when (empty? @titles-by-author)
          [:div
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:author/request-titles-by-author])}
            "View Titles"]])]])))