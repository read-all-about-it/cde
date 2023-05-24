(ns cde.components.metadata
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn chapter-table
  "A table for displaying a list of chapter records (excluding their actual text contents!)"
  [chapters]
  [:table.table.is-hoverable.is-narrow
   [:thead
    [:tr
     [:th "Chapter Number"]
     [:th "Chapter Title"]
     [:th "Publication Date"]]
    [:tbody
     (for [c chapters]
       [:tr
        [:td (get c :chapter_number "")]
        [:td (get c :common_title "None")]
        [:td (get c :final_date "")]])]]])

(defn titles-table
  "A table for displaying a list of title records
   (ie, a list of stories written by a given author,
   or appearing in a given newspaper etc.)"
  [titles]
  [:table.table.is-hoverable.is-fullwidth
   [:thead
    [:tr
     [:th "Title"]
     [:th "Start Date"]
     [:th "End Date"]
     [:th "Newspaper"]]]
   [:tbody
    (for [title titles]
      [:tr
       [:td [:a {:href (str "/#/title/" (get title :id))} (get title :common_title "")]]
       [:td (get title :span_start "")]
       [:td (get title :span_end "")]
       [:td [:a {:href (str "/#/newspaper/" (get title :newspaper_table_id))} (get title :newspaper_common_title "")]]])]])

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

(defn simple-metadata-block
  "A table of styled metadata for a newspaper, author, title, or chapter.
   Takes a map of metadata k-v pairs to display. Optionally, takes an ordered
   vector of keys to display, and will display only those keys.
   Optionally also takes pretty-names for the keys, as a map of keyword/string pairs."
  ([metadata]
   [:table.table.is-hoverable.is-fullwidth
    [:tbody
     (for [[k v] metadata]
       [:tr
        [:th (str/replace (str (name k)) "_" " ")]
        [:td v]])]])
  ([metadata keys-to-display]
   [:table.table.is-hoverable.is-fullwidth
    [:tbody
     (for [k keys-to-display]
       [:tr
        [:th (str/replace (str (name k)) "_" " ")]
        [:td (get metadata k)]])]])
  ([metadata keys-to-display pretty-names]
   [:table.table.is-hoverable.is-fullwidth
    [:tbody
     (for [k keys-to-display]
       [:tr
        [:th (get pretty-names k (str/replace (str (name k)) "_" " "))]
        [:td (get metadata k)]])]]))