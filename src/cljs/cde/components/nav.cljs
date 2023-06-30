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
           [nav-link "#/faq" "FAQ" :faq]
           [nav-link "#/team" "Team" :team]
           [nav-link "#/about" "TBC Explained" :about]]]
         [:div.navbar-start
          [nav-link "#/search" "Explore" :search]
          [nav-link "#/contribute" "Contribute" :contribute]
          [nav-dropdown "About"
           [nav-link "#/faq" "FAQ" :faq]
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

(defn button-group
  "A group of buttons displayed at the top of a page, side by side, centered."
  [& buttons]
  [:div.buttons.is-centered
   (for [button buttons]
     button)])


(defn record-buttons
  "Buttons to display at the top of a 'record' page (for a specific author/newspaper/title/chapter)"
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               page-id (rf/subscribe [:common/page-id])]
    [:div.block.has-text-centered
     (cond (str/includes? (str @page-id) "title") [button-group [add-chapter-to-title-button] [edit-metadata-of-title-button]]
           (str/includes? (str @page-id) "chapter") [button-group [edit-metadata-of-chapter-button]]
           (str/includes? (str @page-id) "author") [:p @page-id]
           (str/includes? (str @page-id) "newspaper") [:p @page-id]
           :else [:p "No buttons for this page."])]))

(defn page-header
  ([title]
   [:div.block.has-text-centered
    [:h1.title.is-3 title]])
  ([title subtitle]
   [:div.block.has-text-centered
    [:h1.title.is-3 title]
    [:h2.subtitle.is-5 subtitle]]))