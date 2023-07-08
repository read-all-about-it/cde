(ns cde.components.metadata
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn basic-chapter-table
  "A table for displaying a list of chapter records (excluding their actual text contents!)"
  [chapters]
  [:table.table.is-hoverable.is-narrow
   [:thead
    [:tr
     [:th "Chapter Number"]
     [:th "Chapter Title"]
     [:th "Publication Date"]]]
    [:tbody
     (for [c chapters]
       [:tr
        [:td [:a {:href (str "/#/chapter/" (get c :id))} (get c :chapter_number "")]]
        [:td (get c :common_title "")]
        [:td (get c :final_date "")]])]])

(defn chapter-table
  "A table for displaying a list of chapter records
  as pre-processed by the records->table-data utility function"
  [chapter-records]
  [:table.table.is-hoverable.is-fullwidth
   [:thead
    [:tr
     [:th "Test"]]]
   [:tbody
    [:tr
     [:td "Test"]]]])

(defn titles-table
  "A table for displaying a list of title records
   (ie, a list of stories written by a given author,
   or appearing in a given newspaper etc.)"
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
      [:tr
       [:td [:a {:href (str "/#/title/" (get title :id))} (get title :common_title "")]]
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
         [:tr
          [:td [:a {:href (str "/#/title/" (get title :id))} (get title :common_title "")]]
          [:td [:a {:href (str "/#/author/" (get title :author_id))} (get title :attributed_author_name (get title :author_common_name ""))]]
          [:td (get title :span_start "")]
          [:td (get title :span_end "")]])]])))

(defn metadata-table
  "A table, generated from a vec of maps. Each map should have
   {:title :value}. Optionally can also have: {:help-text :link}
   If link is non-nil, the value field will be clickable and link to the given url.
   If help-text is non-nil, helper message will display on title."
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


(defn- adding-or-updating-to-metadata
  "A metadata block for 'add' and 'update' pages. Takes a map of details for one of:
   1. a title (if you're adding a chapter to a title)
   2. a newspaper (if you're adding a title to a newspaper)
   
   and a 'metadata-type' keyword, which should be one of: :title, :newspaper
   
   Transforms the metadata into a div showing key details of the title/newspaper
   that you're adding a record to. Intended to sit above a form for adding/updating records."
  [metadata metadata-type]
  [:div.block
   [:p "Test"]] ;; TODO: add metadata here
  )

(defn adding-to-title
  "A block for giving the user a summary of the title they're adding the chapter to."
  [title-metadata] ;; a map of details about a given title
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