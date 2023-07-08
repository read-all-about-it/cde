(ns cde.views.author
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table titles-table]]
   [cde.utils :refer [details->metadata]]
   [cde.components.login :refer [auth0-login-to-edit-button]]
   [cde.components.editing-records :refer [edit-author-form]]
   [cde.components.nav :refer [page-header record-buttons]]))

(defn author-page
  []
  (r/with-let [metadata-loading? (rf/subscribe [:author/metadata-loading?])
               titles-loading? (rf/subscribe [:author/titles-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               author-metadata (rf/subscribe [:author/details])
               titles-by-author (rf/subscribe [:author/titles])
               error (rf/subscribe [:author/error])]
    (fn []
      [:section.section>div.container>div.content
       [:div
        (when (and (not @error) (not @metadata-loading?) (not @author-metadata))
          (rf/dispatch [:author/get-author]))
        
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


(defn edit-an-author
  "View for editing an existing author in the database."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               author-details (rf/subscribe [:author/details])]
    (fn []
      [:section.section>div.container>div.content
       (cond
         (:common_name @author-details) [page-header "Edit An Author"
                                         [:a {:href (str "#/author/" (:id @author-details))}
                                          (:common_name @author-details)]]
         :else [page-header "Edit An Author"])
       (if @logged-in?
         [edit-author-form]
         [auth0-login-to-edit-button])])))