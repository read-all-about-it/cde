(ns cde.views.title
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.creating-records :refer [new-title-form]]
   [cde.components.editing-records :refer [edit-title-form]]
   [cde.components.metadata :refer [metadata-table basic-chapter-table chapter-table]]
   [cde.utils :refer [details->metadata
                      records->table-data]]
   [cde.components.nav :refer [page-header record-buttons]]
   ))


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
        (when (and (not @error) (not @metadata-loading?) (not @title-metadata))
          (rf/dispatch [:title/get-title]))

        (when (and (not (nil? @title-metadata)) (not @metadata-loading?))
          [page-header (or (:publication_title @title-metadata) (:common_title @title-metadata))])
        [record-buttons]

        (when (and (not (nil? @title-metadata)) (not @metadata-loading?))
          [:h3 {:style {:text-align "center"}} "Title Metadata"])
        (when (and (not (nil? @title-metadata)) (not @metadata-loading?))
          [metadata-table (details->metadata @title-metadata :title)])
        (cond
          (true? @chapters-loading?) ;; we're loading chapters, so show a progress bar
          [:progress.progress.is-small.is-primary {:max "100"}]

          (and (false? @chapters-loading?) (empty? @chapters-in-title)) ;; *tried* to load chapters, and there weren't any
          [:div [:h3 {:style {:text-align "center"}} "No Chapters Found in this title record."]]

          (seq @chapters-in-title) ;; we have chapters to display
          [:div
           [:h3 {:style {:text-align "center"}} "Discovered Chapters"]
           [basic-chapter-table @chapters-in-title] ;; switch this for [chapter-table (records->table-data @chapters-in-title :chapter)] when it's ready
           ]

          :else ;; we need to try loading chapters
          [:div.block.has-text-centered
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:title/get-chapters-in-title])}
            "View Chapters"]])]])))

(defn create-a-title
  "View for adding a new title (ie, a new story) to the database."
  []
    (fn []
      [:section.section>div.container>div.content
       [:div
        [page-header "Add A Title"]
        [new-title-form]
        ]]))


(defn edit-a-title
  "View for editing an existing title in the database."
  []
  (r/with-let [title-details (rf/subscribe [:title/details])]
  (fn []
    [:section.section>div.container>div.content
     (cond
       (:publication_title @title-details) [page-header "Edit A Title"
                                            [:a {:href (str "#/title/" (:id @title-details))}
                                             (:publication_title @title-details)]]
       (:common_title @title-details) [page-header "Edit A Title"
                                       [:a {:href (str "#/title/" (:id @title-details))}
                                        (:common_title @title-details)]]
       :else [page-header "Edit A Title"])
     [edit-title-form]])))