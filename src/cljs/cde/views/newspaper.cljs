(ns cde.views.newspaper
  "Newspaper entity views: display, create, and edit.

  Provides pages for viewing newspaper details and titles published within,
  creating new newspaper records, and editing existing newspapers.

  Routes:
  - `#/newspaper/:id` - [[newspaper-page]] - View newspaper details
  - `#/add/newspaper` - [[create-a-newspaper]] - Create new newspaper
  - `#/edit/newspaper/:id` - [[edit-a-newspaper]] - Edit existing newspaper"
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
   [cde.components.forms.editing :refer [edit-newspaper-form]]))

(defn newspaper-page
  "Displays a newspaper's details and titles published within it.

  Shows newspaper metadata (title, location, dates, etc.) and provides
  a button to load titles that were published in this newspaper.

  Handles loading, error, and not-found states consistently:
  - Shows 'Loading...' header and progress bar while fetching
  - Shows error notification if fetch fails
  - Shows 'Not Found' message if newspaper doesn't exist
  - Shows metadata and titles button when loaded successfully"
  []
  (r/with-let [loading? (rf/subscribe [:newspaper/metadata-loading?])
               titles-loading? (rf/subscribe [:newspaper/titles-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               newspaper (rf/subscribe [:newspaper/details])
               titles-in-newspaper (rf/subscribe [:newspaper/titles])
               error (rf/subscribe [:newspaper/error])]
    (fn []
      (let [newspaper-loaded? (seq @newspaper)]
        [:section.section>div.container>div.content
         [:div
          ;; Dispatch to load newspaper if needed (side effect; not rendered)
          (when (and (not @error) (not @loading?) (not newspaper-loaded?))
            (rf/dispatch [:newspaper/get-newspaper]))

          ;; Page header - shows appropriate text for each state
          (cond
            @error [page-header "Error Loading Newspaper"]
            @loading? [page-header "Newspaper Loading..."]
            newspaper-loaded? [page-header (:title @newspaper)]
            :else [page-header "Newspaper Not Found"])

          ;; Status feedback: error notification or progress bar while loading
          (cond
            @error [:div.notification.is-danger
                    [:p [:strong "An error occurred: "] (str @error)]]
            @loading? [:progress.progress.is-small.is-primary {:max 100}]
            :else [record-buttons])

          ;; Newspaper metadata (only shown when loaded)
          (when newspaper-loaded?
            [:div
             [:h3 {:style {:text-align "center"}} "Newspaper Metadata"]
             [metadata-table (details->metadata @newspaper :newspaper)]])

          ;; Titles section (only shown when newspaper is loaded)
          (when newspaper-loaded?
            (cond
              @titles-loading?
              [:progress.progress.is-small.is-primary {:max 100}]

              (and (false? @titles-loading?) (empty? @titles-in-newspaper))
              [:div [:h3 {:style {:text-align "center"}} "No Titles Found in this newspaper record."]]

              (seq @titles-in-newspaper)
              [:div
               [:h3 {:style {:text-align "center"}} "Titles in Newspaper"]
               [titles-table @titles-in-newspaper :newspaper]]

              :else
              [:div.block.has-text-centered
               [:button.button.is-primary
                {:on-click #(rf/dispatch [:newspaper/get-titles-in-newspaper])}
                "View Titles"]]))]]))))

(defn create-a-newspaper
  "Renders the form for creating a new newspaper record.

  Displays the new-newspaper-form component. Requires authentication."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])]
    [:section.section>div.container>div.content
     [:div
      [page-header "Add A Newspaper"]
      (if @logged-in?
        [new-newspaper-form]
        [auth0-login-to-edit-button])]]))

(defn edit-a-newspaper
  "Renders the newspaper editing form for authenticated users.

  Displays the edit form for modifying an existing newspaper's metadata.
  Requires authentication; shows login prompt for unauthenticated users."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])]
    [:section.section>div.container>div.content
     [:div
      [page-header "Edit A Newspaper"]
      (if @logged-in?
        [edit-newspaper-form]
        [auth0-login-to-edit-button])]]))
