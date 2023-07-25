(ns cde.views.newspaper
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.login :refer [auth0-login-to-edit-button]]
   [cde.components.metadata :refer [metadata-table titles-table]]
   [cde.components.nav :refer [page-header record-buttons]]
   [cde.utils :refer [details->metadata]]
   [cde.components.forms.creation :refer [new-newspaper-form]]
   [cde.components.editing-records :refer [edit-newspaper-form]]))


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
        (when (and (not @error) (not @metadata-loading?) (not @newspaper-metadata))
          (rf/dispatch [:newspaper/get-newspaper]))

        (when (and (not (nil? @newspaper-metadata)) (not @metadata-loading?))
          [page-header (:title @newspaper-metadata)])

        [record-buttons]

        (when (and (not (nil? @newspaper-metadata)) (not @metadata-loading?))
          [:h3 {:style {:text-align "center"}} "Newspaper Metadata"])
        (when (and (not (nil? @newspaper-metadata)) (not @metadata-loading?))
          [metadata-table (details->metadata @newspaper-metadata :newspaper)])
        (cond
          (true? @titles-loading?) ;; we're loading titles, so show a progress bar
          [:progress.progress.is-small.is-primary {:max "100"}]

          (and (false? @titles-loading?) (empty? @titles-in-newspaper)) ;; *tried* to load titles, and there weren't any
          [:div [:h3 {:style {:text-align "center"}} "No Titles Found in this newspaper record."]]

          (seq @titles-in-newspaper) ;; we have titles to display
          [:div
           [:h3 {:style {:text-align "center"}} "Titles in Newspaper"]
           [titles-table @titles-in-newspaper :newspaper]]

          :else ;; we need to try loading titles
          [:div.block.has-text-centered
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:newspaper/get-titles-in-newspaper])}
            "View Titles"]])]])))

(defn create-a-newspaper
  "View for creating a new newspaper record."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])]
    [:section.section>div.container>div.content
     [:div
      [page-header "Add A Newspaper"]
      (if @logged-in?
        [new-newspaper-form]
        [auth0-login-to-edit-button])]]))

(defn edit-a-newspaper
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])]
    [:section.section>div.container>div.content
     [:div
      [page-header "Edit A Newspaper"]
      (if @logged-in?
        [edit-newspaper-form]
        [auth0-login-to-edit-button])]]))