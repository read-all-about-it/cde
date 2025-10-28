(ns cde.components.metadata
  "Components for displaying record metadata in tables.

  Provides table components for rendering entity details and lists:
  - [[metadata-table]] - Key-value display for single record details
  - [[titles-table]] - List display for titles (by author or in newspaper)
  - [[basic-chapter-table]] - List display for chapters in a title

  These components work with data transformed by [[cde.utils/details->metadata]]."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn basic-chapter-table
  "Displays a list of chapters in a simple table format.

  Shows chapter number, title, and publication date. Rows are clickable
  to navigate to the chapter detail page.

  Arguments:
  - `chapters` - vector of chapter record maps"
  [chapters]
  [:table.table.is-hoverable.is-narrow
   [:thead
    [:tr
     [:th "Chapter Number"]
     [:th "Chapter Title"]
     [:th "Publication Date"]]]
   [:tbody
    (for [c chapters]
      [:tr {:style {:cursor "pointer"}
            :on-click #(set! (.-location js/window) (str "/#/chapter/" (get c :id)))}
       [:td (get c :chapter_number "")]
       [:td (or (get c :common_title) (get c :chapter_title) "")]
       [:td (get c :final_date "Date Unknown")]])]])

(defn titles-table
  "Displays a list of titles in a table format.

  Shows title name, newspaper/author (based on focus), and date range.
  Rows are clickable to navigate to the title detail page.

  Arguments:
  - `titles` - vector of title record maps
  - `focus` - optional keyword; when `:newspaper`, shows author column
              instead of newspaper column (for newspaper detail pages)"
  ([titles]
   [:table.table.is-hoverable.is-fullwidth
    [:thead
     [:tr
      [:th "Title"]
      [:th "Newspaper"]
      [:th "Start Date"]
      [:th "End Date"]]]
    [:tbody
     (for [title titles]
       [:tr {:style {:cursor "pointer"}
             :on-click #(set! (.-location js/window) (str "/#/title/" (get title :id)))}
        [:td [:a {:href (str "/#/title/" (get title :id))} (or (not-empty (get title :common_title)) (not-empty (get title :publication_title)) "Unknown Title")]]
        [:td [:a {:href (str "/#/newspaper/" (get title :newspaper_table_id))} (get title :newspaper_title "")]]
        [:td (get title :span_start "")]
        [:td (get title :span_end "")]])]])
  ([titles focus]
   ;; if 'focus' is :newspaper, then we're displaying titles in a given newspaper (and so we should include *author's* name, but not newspaper name)
   ;; otherwise we're displaying titles by a given author (and so should include the newspaper's name in table, not the author's)
   (if (not= focus :newspaper)
     (titles-table titles)
     [:table.table.is-hoverable.is-fullwidth
      [:thead
       [:tr
        [:th "Title"]
        [:th "Author"]
        [:th "Start Date"]
        [:th "End Date"]]]
      [:tbody
       (for [title titles]
         [:tr {:style {:cursor "pointer"}
               :on-click #(set! (.-location js/window) (str "/#/title/" (get title :id)))}
          [:td [:a {:href (str "/#/title/" (get title :id))} (or (not-empty (get title :common_title)) (not-empty (get title :publication_title)) "Unknown Title")]]
          [:td [:a {:href (str "/#/author/" (get title :author_id))} (or (not-empty (get title :attributed_author_name)) (not-empty (get title :author_common_name)) "Unknown Author")]]
          [:td (get title :span_start "")]
          [:td (get title :span_end "")]])]])))

(defn metadata-table
  "Displays entity metadata as a vertical key-value table.

  Each row shows a field label and its value. Supports optional
  links and row highlighting.

  Arguments:
  - `metadata` - vector of maps with keys:
    - `:title` - field label
    - `:value` - field value (can be hiccup)
    - `:link` - optional URL to make value clickable
    - `:highlight` - optional boolean for row highlighting"
  [metadata]
  [:table.table.is-hoverable.is-fullwidth
   [:tbody
    (for [m metadata]
      [:tr
       (when (:highlight m) {:class "is-selected"})
       [:th (:title m)]
       (if-not (nil? (:link m))
         [:td [:a {:href (:link m)} (:value m)]]
         [:td (:value m)])])]])

(defn adding-to-title
  "Displays context information when adding a chapter to an existing title.

  Shows the title name, author, and newspaper as links when available.
  Used on the create-chapter form.

  Arguments:
  - `title-metadata` - map of title details including author and newspaper info"
  [title-metadata]
  [:div.block
   [:h5 {:style {:text-align "center"}}

    (when (and (or (:id title-metadata) (:title_id title-metadata))
               (or (:publication_title title-metadata) (:common_title title-metadata)))
      [:span
       [:span "Adding to "]
       [:span [:a {:href (str "/#/title/" (or (:id title-metadata)
                                              (:title_id title-metadata)))}
               (or (:publication_title title-metadata)
                   (:common_title title-metadata))]]])

    (when (and (or (:id title-metadata) (:title_id title-metadata))
               (or (:publication_title title-metadata) (:common_title title-metadata))
               (:author_id title-metadata)
               (or (:attributed_author_name title-metadata) (:author_common_name title-metadata)))
      [:span " by "])

    (when (and (:author_id title-metadata)
               (or (:attributed_author_name title-metadata) (:author_common_name title-metadata)))
      [:span [:a {:href (str "/#/author/" (:author_id title-metadata))}
              (or (:attributed_author_name title-metadata)
                  (:author_common_name title-metadata))]])

    [:br]

    (when (and (:newspaper_table_id title-metadata)
               (or (:newspaper_title title-metadata) (:newspaper_common_title title-metadata)))
      [:span
       [:span "As published in "]
       [:span [:a {:href (str "/#/newspaper/" (:newspaper_table_id title-metadata))}
               (or (:newspaper_title title-metadata)
                   (:newspaper_common_title title-metadata))]]])]])
