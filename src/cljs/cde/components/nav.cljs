(ns cde.components.nav
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.components.login :as login]
   [clojure.string :as str]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn nav-dropdown [title & links]
  (r/with-let [dropdown-expanded? (r/atom false)]
    [:div.navbar-item.has-dropdown.is-hoverable
     [:a.navbar-link
      {:on-click #(swap! dropdown-expanded? not)
       :class (when @dropdown-expanded? :is-active)}
      title]
     [:div.navbar-dropdown
      {:class (when @dropdown-expanded? :is-active)}
      links]]))

(defn navbar []
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


(defn add-chapter-to-title-button
  "Button to add a new chapter to an existing title"
  []
  (r/with-let [title-details (rf/subscribe [:title/details])]
    ;; link to #/add/chapter?title_id=123 where 123 is from (:id @title-details)
    [:a.button.is-primary
     {:href (if-not (:id @title-details)
              (str "#/add/chapter")
              (str "#/add/chapter?title_id=" (:id @title-details)))}
     [:span "Add Chapter"]]))

(defn add-title-by-author-button
  "Button to add a new chapter to an existing title"
  []
  (r/with-let [author-details (rf/subscribe [:author/details])]
    ;; link to #/add/title?author_id=123 where 123 is from (:id @author-details)
    [:a.button.is-primary
     {:href (if-not (:id @author-details)
              (str "#/add/title")
              (str "#/add/title?author_id=" (:id @author-details)))}
     [:span "Add A New Title By This Author"]]))

(defn add-title-in-newspaper-button
  "Button to add a new chapter to an existing title"
  []
  (r/with-let [newspaper-details (rf/subscribe [:newspaper/details])]
    ;; link to #/add/title?newspaper_table_id=123 where 123 is from (:id @newspaper-details)
    [:a.button.is-primary
     {:href (if-not (:id @newspaper-details)
              (str "#/add/title")
              (str "#/add/title?newspaper_table_id=" (:id @newspaper-details)))}
     [:span "Add A New Title In This Newspaper"]]))

(defn edit-metadata-of-title-button
  "Button to edit the metadata for a title"
  []
  (r/with-let [title-details (rf/subscribe [:title/details])]
    [:a.button.button.is-primary
     {:href (str "#/edit/title/" (:id @title-details))}
     [:span "Edit Metadata"]]))

(defn edit-metadata-of-chapter-button
  "Button to edit the metadata for a chapter"
  []
  (r/with-let [chapter-details (rf/subscribe [:chapter/details])]
    [:a.button.button.is-primary
     {:href (str "#/edit/chapter/" (:id @chapter-details))}
     [:span "Edit Metadata"]]))

(defn edit-metadata-of-author-button
  "Button to edit the metadata of an author"
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
  "Button to update the details of a chapter from Trove"
  []
  (r/with-let [chapter-details (rf/subscribe [:chapter/details])]
    [:a.button.button.is-primary
     {:on-click #(rf/dispatch [:trove/put-chapter (:trove_article_id @chapter-details)])}
     [:span "Update Text From Trove"]]))

(defn button-group
  "A group of buttons displayed at the top of a page, side by side, centered."
  [& buttons]
  [:div.buttons.is-centered
   (for [button buttons]
     button)])

(defn contribute-block
  "A group of buttons to add a title, chapter, author, or newspaper."
  []
  [button-group
   [:a.button.is-primary {:href "#/add/title"} "Add A Title"]
   [:a.button.is-primary {:href "#/add/chapter"} "Add A Chapter"]
   [:a.button.is-primary {:href "#/add/author"} "Add An Author"]
   [:a.button.is-primary {:href "#/add/newspaper"} "Add A Newspaper"]
   ])

(defn record-buttons
  "Buttons to display at the top of a 'record' page (for a specific author/newspaper/title/chapter)"
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
          ;; [edit-metadata-of-newspaper-button]
          ]
         :else [:p "No buttons for this page."])]))

(defn page-header
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