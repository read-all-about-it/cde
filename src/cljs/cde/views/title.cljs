(ns cde.views.title
  "Title (serialised fiction work) entity views: display, create, and edit.

  Provides pages for viewing title details and their chapters,
  creating new title records, and editing existing titles.

  Routes:
  - `#/title/:id` - [[title-page]] - View title details and chapters
  - `#/add/title` - [[create-a-title]] - Create new title
  - `#/edit/title/:id` - [[edit-a-title]] - Edit existing title"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.login :refer [auth0-login-to-edit-button]]
   [cde.components.forms.creation :refer [new-title-form]]
   [cde.components.forms.editing :refer [edit-title-form]]
   [cde.components.metadata :refer [metadata-table basic-chapter-table]]
   [cde.utils :refer [details->metadata
                      records->table-data]]
   [cde.components.nav :refer [page-header record-buttons]]))

(defn- ^:no-doc title-display-name
  "Returns the best available display name for a title.

  Prefers `:publication_title`, falls back to `:common_title`."
  [title]
  (or (:publication_title title) (:common_title title)))

(defn title-page
  "Displays a title's details and its chapters.

  Shows title metadata (publication title, author, dates, newspaper, etc.)
  and provides a button to load chapters belonging to the title.

  Handles loading, error, and not-found states consistently:
  - Shows 'Loading...' header and progress bar while fetching
  - Shows error notification if fetch fails
  - Shows 'Not Found' message if title doesn't exist
  - Shows metadata and chapters button when loaded successfully"
  []
  (r/with-let [loading? (rf/subscribe [:title/metadata-loading?])
               chapters-loading? (rf/subscribe [:title/chapters-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               title (rf/subscribe [:title/details])
               chapters-in-title (rf/subscribe [:title/chapters])
               error (rf/subscribe [:title/error])]
    (fn []
      (let [title-loaded? (seq @title)]
        [:section.section>div.container>div.content
         [:div
          ;; Dispatch to load title if needed (side effect; not rendered)
          (when (and (not @error) (not @loading?) (not title-loaded?))
            (rf/dispatch [:title/get-title]))

          ;; Page header - shows appropriate text for each state
          (cond
            @error [page-header "Error Loading Title"]
            @loading? [page-header "Title Loading..."]
            title-loaded? [page-header (title-display-name @title)]
            :else [page-header "Title Not Found"])

          ;; Status feedback: error notification or progress bar while loading
          (cond
            @error [:div.notification.is-danger
                    [:p [:strong "An error occurred: "] (str @error)]]
            @loading? [:progress.progress.is-small.is-primary {:max 100}]
            :else [record-buttons])

          ;; Title metadata (only shown when loaded)
          (when title-loaded?
            [:div
             [:h3 {:style {:text-align "center"}} "Title Metadata"]
             [metadata-table (details->metadata @title :title)]])

          ;; Chapters section (only shown when title is loaded)
          (when title-loaded?
            (cond
              @chapters-loading?
              [:progress.progress.is-small.is-primary {:max 100}]

              (and (false? @chapters-loading?) (empty? @chapters-in-title))
              [:div [:h3 {:style {:text-align "center"}} "No Chapters Found in this title record."]]

              (seq @chapters-in-title)
              [:div
               [:h3 {:style {:text-align "center"}} "Discovered Chapters"]
               [basic-chapter-table @chapters-in-title]]

              :else
              [:div.block.has-text-centered
               [:button.button.is-primary
                {:on-click #(rf/dispatch [:title/get-chapters-in-title])}
                "View Chapters"]]))]]))))

(defn create-a-title
  "Renders the form for creating a new title (serialised fiction work).

  Displays the new-title-form component for adding titles to the database."
  []
  (fn []
    [:section.section>div.container>div.content
     [:div
      [page-header "Add A Title"]
      [new-title-form]]]))

(defn edit-a-title
  "Renders the title editing form for authenticated users.

  Displays the edit form for modifying an existing title's metadata.
  Requires authentication; shows login prompt for unauthenticated users."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               title-details (rf/subscribe [:title/details])]
    (fn []
      [:section.section>div.container>div.content
       (if-let [display-name (title-display-name @title-details)]
         [page-header "Edit A Title"
          [:a {:href (str "#/title/" (:id @title-details))}
           display-name]]
         [page-header "Edit A Title"])
       (if @logged-in?
         [edit-title-form]
         [auth0-login-to-edit-button])])))
