(ns cde.pages.author
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table titles-table]]
   [cde.utils :refer [details->metadata]]
   [cde.components.nav :refer [page-header record-buttons]]))

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
        (when (and (not (nil? @author-metadata)) (not @metadata-loading?))
          [page-header (:common_name @author-metadata)])
        
        [record-buttons]

        (when (and (not (nil? @author-metadata)) (not @metadata-loading?))
          [:h3 {:style {:text-align "center"}} "Author Metadata"])
        (when (and (not (nil? @author-metadata)) (not @metadata-loading?))
          [metadata-table (details->metadata @author-metadata :author)])
        (cond
          (true? @titles-loading?) ;; we're loading titles, so show a progress bar
          [:progress.progress.is-small.is-primary {:max "100"}]

          (and (false? @titles-loading?) (empty? @titles-by-author)) ;; *tried* to load titles, and there weren't any
          [:div [:h3 {:style {:text-align "center"}} "No Titles Found for this author record."]]

          (seq @titles-by-author) ;; we have titles to display
          [:div
           [:h3 {:style {:text-align "center"}} "Attributed Titles"]
           [titles-table @titles-by-author]]

          :else ;; we need to try loading titles
          [:div.block.has-text-centered
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:author/request-titles-by-author])}
            "View Titles"]])]])))