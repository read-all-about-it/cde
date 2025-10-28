(ns cde.components.nav
  "Navigation and page layout components.

  Provides the main navigation bar, page headers, and context-sensitive
  action buttons for record pages. Components adapt based on the current
  route and user authentication status.

  Key components:
  - [[navbar]] - Main site navigation with responsive mobile menu
  - [[page-header]] - Consistent page title display
  - [[record-buttons]] - Context-sensitive action buttons for entity pages
  - [[contribute-block]] - Quick links for adding new records"
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.components.login :as login]
   [clojure.string :as str]))

;;;; Navigation Bar Components

(defn nav-link
  "Renders a navigation link with active state highlighting.

  Arguments:
  - `uri` - link destination
  - `title` - display text
  - `page` - page keyword for active state comparison"
  [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn nav-dropdown
  "Renders a dropdown menu in the navigation bar.

  Arguments:
  - `title` - dropdown trigger text
  - `links` - child nav-link components"
  [title & links]
  (r/with-let [dropdown-expanded? (r/atom false)]
    [:div.navbar-item.has-dropdown.is-hoverable
     [:a.navbar-link
      {:on-click #(swap! dropdown-expanded? not)
       :class (when @dropdown-expanded? :is-active)}
      title]
     [:div.navbar-dropdown
      {:class (when @dropdown-expanded? :is-active)}
      links]]))

(defn navbar
  "Main site navigation bar with responsive mobile menu.

  Includes navigation links, dropdown menus, and authentication
  controls (login/logout buttons, user nameplate). Uses Bulma
  navbar classes with burger menu for mobile responsiveness."
  []
  (r/with-let [burger-expanded? (r/atom false)
               user (rf/subscribe [:auth/user])]
    [:nav.navbar.is-transparent.is-spaced.has-shadow
     [:div.container
      [:div.navbar-brand
       [:a.navbar-item {:href "/" :style {:font-weight :bold}} [:i.material-icons "newspaper"]]
       [:span.navbar-burger.burger
        {:data-target :nav-menu
         :on-click #(swap! burger-expanded? not)
         :class (when @burger-expanded? :is-active)}
        [:span] [:span] [:span]]]
      [:div#nav-menu.navbar-menu
       {:class (when @burger-expanded? :is-active)}
       (if-some [user @(rf/subscribe [:auth/user])]
         [:div.navbar-start
          [nav-link "#/search" "Explore" :search]
          [nav-link "#/contribute" "Contribute" :contribute]
          [nav-dropdown "About"
          ;;  [nav-link "#/faq" "FAQ" :faq]
           [nav-link "#/team" "Team" :team]
           [nav-link "#/about" "TBC Explained" :about]]]
         [:div.navbar-start
          [nav-link "#/search" "Explore" :search]
          [nav-link "#/contribute" "Contribute" :contribute]
          [nav-dropdown "About"
          ;;  [nav-link "#/faq" "FAQ" :faq]
           [nav-link "#/team" "Team" :team]
           [nav-link "#/about" "TBC Explained" :about]]])
       [:div.navbar-end
        [:div.navbar-item
         (if-some [user @(rf/subscribe [:auth/user])]
           [:div.buttons
            [login/nameplate user]
            [login/auth0-logout-button]]
           [:div.buttons
            [login/auth0-login-button]])]]]]]))

;;;; Record Action Buttons

(defn add-chapter-to-title-button
  "Button linking to add a new chapter to the current title.

  Includes `title_id` query parameter when title details are available."
  []
  (r/with-let [title-details (rf/subscribe [:title/details])]
    ;; link to #/add/chapter?title_id=123 where 123 is from (:id @title-details)
    [:a.button.is-primary
     {:href (if-not (:id @title-details)
              (str "#/add/chapter")
              (str "#/add/chapter?title_id=" (:id @title-details)))}
     [:span "Add Chapter"]]))

(defn add-title-by-author-button
  "Button linking to add a new title by the current author.

  Includes `author_id` query parameter when author details are available."
  []
  (r/with-let [author-details (rf/subscribe [:author/details])]
    ;; link to #/add/title?author_id=123 where 123 is from (:id @author-details)
    [:a.button.is-primary
     {:href (if-not (:id @author-details)
              (str "#/add/title")
              (str "#/add/title?author_id=" (:id @author-details)))}
     [:span "Add A New Title By This Author"]]))

(defn add-title-in-newspaper-button
  "Button linking to add a new title in the current newspaper.

  Includes `newspaper_table_id` query parameter when newspaper details are available."
  []
  (r/with-let [newspaper-details (rf/subscribe [:newspaper/details])]
    ;; link to #/add/title?newspaper_table_id=123 where 123 is from (:id @newspaper-details)
    [:a.button.is-primary
     {:href (if-not (:id @newspaper-details)
              (str "#/add/title")
              (str "#/add/title?newspaper_table_id=" (:id @newspaper-details)))}
     [:span "Add A New Title In This Newspaper"]]))

(defn edit-metadata-of-title-button
  "Button linking to edit the current title's metadata."
  []
  (r/with-let [title-details (rf/subscribe [:title/details])]
    [:a.button.button.is-primary
     {:href (str "#/edit/title/" (:id @title-details))}
     [:span "Edit Metadata"]]))

(defn edit-metadata-of-chapter-button
  "Button linking to edit the current chapter's metadata."
  []
  (r/with-let [chapter-details (rf/subscribe [:chapter/details])]
    [:a.button.button.is-primary
     {:href (str "#/edit/chapter/" (:id @chapter-details))}
     [:span "Edit Metadata"]]))

(defn edit-metadata-of-author-button
  "Button linking to edit the current author's metadata."
  []
  (r/with-let [author-details (rf/subscribe [:author/details])]
    [:a.button.button.is-primary
     {:href (str "#/edit/author/" (:id @author-details))}
     [:span "Edit Metadata"]]))

(defn edit-metadata-of-newspaper-button
  "Button to edit the metadata of a newspaper"
  []
  (r/with-let [newspaper-details (rf/subscribe [:newspaper/details])]
    [:a.button.button.is-primary
     {:href (str "#/edit/newspaper/" (:id @newspaper-details))}
     [:span "Edit Metadata"]]))

(defn view-chapter-on-trove-button
  "Button to view a chapter on Trove.

   Note: unique among record nav buttons, this opens in a new tab where possible."
  []
  (r/with-let [chapter-details (rf/subscribe [:chapter/details])]
    [:a.button.button.is-primary
     {:href (str "https://trove.nla.gov.au/newspaper/article/" (:trove_article_id @chapter-details))
      :target "_blank"}
     [:span "View On Trove"]]))

(defn update-from-trove-button
  "Button to refresh chapter text and details from the Trove API."
  []
  (r/with-let [chapter-details (rf/subscribe [:chapter/details])]
    [:a.button.button.is-primary
     {:on-click #(rf/dispatch [:trove/put-chapter (:trove_article_id @chapter-details)])}
     [:span "Update Text From Trove"]]))

;;;; Button Groups and Page Components

(defn button-group
  "Renders a horizontally centered group of buttons.

  Arguments:
  - `buttons` - variadic button components to display"
  [& buttons]
  [:div.buttons.is-centered
   (for [button buttons]
     button)])

(defn contribute-block
  "Renders quick-action buttons for creating new records.

  Displays buttons to add titles, chapters, authors, and newspapers.
  Used on the contribute page for authenticated users."
  []
  [button-group
   [:a.button.is-primary {:href "#/add/title"} "Add A Title"]
   [:a.button.is-primary {:href "#/add/chapter"} "Add A Chapter"]
   [:a.button.is-primary {:href "#/add/author"} "Add An Author"]
   [:a.button.is-primary {:href "#/add/newspaper"} "Add A Newspaper"]])

(defn record-buttons
  "Renders context-sensitive action buttons for entity detail pages.

  Displays different buttons based on:
  - Current page type (title, chapter, author, newspaper)
  - User authentication status

  For unauthenticated users, shows a login prompt.
  For chapter pages, includes Trove integration buttons."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               page-id (rf/subscribe [:common/page-id])]
    [:div.block.has-text-centered
     (cond
       (and (not @logged-in?) (str/includes? (str @page-id) "chapter"))
       [button-group
        [view-chapter-on-trove-button]
        [update-from-trove-button]
        [:button.button.is-primary {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])} "Login To Edit"]]
       (not @logged-in?)
       [:button.button.is-primary {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])} "Login To Edit"]
       (str/includes? (str @page-id) "title")
       [button-group [add-chapter-to-title-button] [edit-metadata-of-title-button]]
       (str/includes? (str @page-id) "chapter")
       [button-group
        [view-chapter-on-trove-button]
        [update-from-trove-button]
        [edit-metadata-of-chapter-button]]
       (str/includes? (str @page-id) "author")
       [button-group [add-title-by-author-button] [edit-metadata-of-author-button]]
       (str/includes? (str @page-id) "newspaper")
       [button-group [add-title-in-newspaper-button]
        [edit-metadata-of-newspaper-button]]
       :else [:p "No buttons for this page."])]))

(defn page-header
  "Renders a centered page header with optional subtitle(s).

  Supports multiple arities:
  - `(page-header title)` - title only
  - `(page-header title subtitle)` - title with subtitle
  - `(page-header title subtitle & more)` - title with multiple subtitles

  Arguments:
  - `title` - main page heading
  - `subtitle` - secondary heading (optional)
  - `subs` - additional subheadings (optional)"
  ([title]
   [:div.block.has-text-centered
    [:h1.title.is-3 title]])
  ([title subtitle]
   [:div.block.has-text-centered
    [:h1.title.is-3 title]
    [:h2.subtitle.is-5 subtitle]])
  ([title subtitle & subs]
   [:div.block.has-text-centered
    [:h1.title.is-3 title]
    [:h2.subtitle.is-5 subtitle]
    (for [sub subs]
      [:h3.subsubtitle.is-5 sub])]))
