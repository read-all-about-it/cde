(ns cde.views.chapter
  "Chapter (story instalment) entity views: display, create, and edit.

  Provides pages for viewing chapter details and text content,
  creating new chapters for existing titles, and editing chapter metadata.

  Routes:
  - `#/chapter/:id` - [[chapter-page]] - View chapter details and text
  - `#/add/chapter` - [[create-a-chapter]] - Create new chapter
  - `#/edit/chapter/:id` - [[edit-a-chapter]] - Edit existing chapter"
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.login :refer [auth0-login-to-edit-button]]
   [cde.components.metadata :refer [metadata-table
                                    adding-to-title]]
   [cde.utils :refer [details->metadata]]
   [cde.components.forms.creation :refer [new-chapter-form]]
   [cde.components.forms.editing :refer [edit-chapter-form]]
   [cde.components.nav :refer [page-header record-buttons]]))

(defn- ^:no-doc chapter-display-name
  "Returns the best available display name for a chapter.

  Prefers `:chapter_title`, falls back to `:chapter_number`, then 'Untitled Chapter'."
  [chapter]
  (or (not-empty (:chapter_title chapter))
      (not-empty (:chapter_number chapter))
      "Untitled Chapter"))

(defn- ^:no-doc chapter-text-block
  "Renders chapter HTML content in a container div.

  Arguments:
  - `text` - HTML string from `:chapter_html` field

  Returns: Reagent hiccup rendering the HTML directly via dangerouslySetInnerHTML."
  [text]
  [:div
   [:div {:dangerouslySetInnerHTML {:__html text}}]])

(defn chapter-page
  "Displays a chapter's metadata and full text content.

  Shows chapter details (title, publication date, word count, etc.) and
  renders the chapter text from Trove.

  Handles loading, error, and not-found states consistently:
  - Shows 'Loading...' header and progress bar while fetching
  - Shows error notification if fetch fails
  - Shows 'Not Found' message if chapter doesn't exist
  - Shows metadata and text when loaded successfully"
  []
  (r/with-let [loading? (rf/subscribe [:chapter/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               chapter (rf/subscribe [:chapter/details])
               error (rf/subscribe [:chapter/error])]
    (fn []
      (let [chapter-loaded? (seq @chapter)]
        [:section.section>div.container>div.content
         [:div
          ;; Dispatch to load chapter if needed (side effect; not rendered)
          (when (and (not @error) (not @loading?) (not chapter-loaded?))
            (rf/dispatch [:chapter/get-chapter]))

          ;; Page header - shows appropriate text for each state
          (cond
            @error [page-header "Error Loading Chapter"]
            @loading? [page-header "Chapter Loading..."]
            chapter-loaded? [page-header (chapter-display-name @chapter)]
            :else [page-header "Chapter Not Found"])

          ;; Status feedback: error notification or progress bar while loading
          (cond
            @error [:div.notification.is-danger
                    [:p [:strong "An error occurred: "] (str @error)]]
            @loading? [:progress.progress.is-small.is-primary {:max 100}]
            :else [record-buttons])

          ;; Chapter metadata (only shown when loaded)
          (when chapter-loaded?
            [:div
             [:h3 {:style {:text-align "center"}} "Chapter Details"]
             [metadata-table (details->metadata @chapter :chapter)]])

          ;; Chapter text content (only shown when loaded)
          (when chapter-loaded?
            [:div
             [:h3 {:style {:text-align "center"}} "Chapter Text"]
             [chapter-text-block (:chapter_html @chapter)]])]]))))

(defn create-a-chapter
  "Renders the form for creating a new chapter in an existing title.

  Shows which title the chapter will be added to and displays the
  new-chapter-form. Requires authentication."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               title-details (rf/subscribe [:title/details])]
    (fn []
      [:section.section>div.container>div.content
       [:div
        [page-header "Add A Chapter"]
        [adding-to-title @title-details]
        (if @logged-in?
          [new-chapter-form]
          [auth0-login-to-edit-button])]])))

(defn edit-a-chapter
  "Renders the chapter editing form for authenticated users.

  Displays the edit form for modifying an existing chapter's metadata.
  Requires authentication; shows login prompt for unauthenticated users."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               chapter-details (rf/subscribe [:chapter/details])]
    (fn []
      [:section.section>div.container>div.content
       [page-header "Edit Chapter"
        [:a {:href (str "#/chapter/" (:id @chapter-details))}
         (chapter-display-name @chapter-details)]]
       (if @logged-in?
         [edit-chapter-form]
         [auth0-login-to-edit-button])])))
