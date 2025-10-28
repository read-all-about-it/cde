(ns cde.views.author
  "Author entity views: display, create, and edit.

  Provides pages for viewing author details and their attributed titles,
  creating new author records, and editing existing authors.

  Routes:
  - `#/author/:id` - [[author-page]] - View author details
  - `#/add/author` - [[create-an-author]] - Create new author
  - `#/edit/author/:id` - [[edit-an-author]] - Edit existing author"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table titles-table]]
   [cde.utils :refer [details->metadata]]
   [cde.components.login :refer [auth0-login-to-edit-button]]
   [cde.components.forms.editing :refer [edit-author-form]]
   [cde.components.forms.creation :refer [new-author-form]]
   [cde.components.nav :refer [page-header record-buttons]]))

(defn author-page
  "Displays an author's details and their attributed titles.

  Shows author metadata (name, nationality, gender, etc.) and provides
  a button to load titles written by the author.

  Handles loading, error, and not-found states consistently:
  - Shows 'Loading...' header and progress bar while fetching
  - Shows error notification if fetch fails
  - Shows 'Not Found' message if author doesn't exist
  - Shows metadata and titles button when loaded successfully"
  []
  (r/with-let [loading? (rf/subscribe [:author/metadata-loading?])
               titles-loading? (rf/subscribe [:author/titles-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               author (rf/subscribe [:author/details])
               titles-by-author (rf/subscribe [:author/titles])
               error (rf/subscribe [:author/error])]
    (fn []
      (let [author-loaded? (seq @author)]
        [:section.section>div.container>div.content
         [:div
          ;; Dispatch to load author if needed (side effect; not rendered)
          (when (and (not @error) (not @loading?) (not author-loaded?))
            (rf/dispatch [:author/get-author]))

          ;; Page header - shows appropriate text for each state
          (cond
            @error [page-header "Error Loading Author"]
            @loading? [page-header "Author Loading..."]
            author-loaded? [page-header (:common_name @author)]
            :else [page-header "Author Not Found"])

          ;; Status feedback: error notification or progress bar while loading
          (cond
            @error [:div.notification.is-danger
                    [:p [:strong "An error occurred: "] (str @error)]]
            @loading? [:progress.progress.is-small.is-primary {:max 100}]
            :else [record-buttons])

          ;; Author metadata (only shown when loaded)
          (when author-loaded?
            [:div
             [:h3 {:style {:text-align "center"}} "Author Metadata"]
             [metadata-table (details->metadata @author :author)]])

          ;; Titles section (only shown when author is loaded)
          (when author-loaded?
            (cond
              @titles-loading?
              [:progress.progress.is-small.is-primary {:max 100}]

              (and (false? @titles-loading?) (empty? @titles-by-author))
              [:div [:h3 {:style {:text-align "center"}} "No Titles Found for this author record."]]

              (seq @titles-by-author)
              [:div
               [:h3 {:style {:text-align "center"}} "Attributed Titles"]
               [titles-table @titles-by-author]]

              :else
              [:div.block.has-text-centered
               [:button.button.is-primary
                {:on-click #(rf/dispatch [:author/request-titles-by-author])}
                "View Titles"]]))]]))))

(defn edit-an-author
  "Renders the author editing form for authenticated users.

  Displays the edit form for modifying an existing author's metadata.
  Requires authentication; shows login prompt for unauthenticated users."
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

(defn create-an-author
  "Renders the form for creating a new author record.

  Displays the new-author-form component for adding authors to the database."
  []
  (fn []
    [:section.section>div.container>div.content
     [:div
      [page-header "Add An Author"]
      [new-author-form]]]))
