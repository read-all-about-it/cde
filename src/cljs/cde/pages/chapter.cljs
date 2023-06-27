(ns cde.pages.chapter
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table
                                    adding-to-title]]
   [cde.utils :refer [details->metadata]]
   [cde.components.forms :refer [new-chapter-form]]
   [cde.components.nav :refer [page-header record-buttons]]))



(defn chapter-text-block
  [text]
  [:div
   [:div {:dangerouslySetInnerHTML {:__html text}}]])


(defn chapter-page
  []
  (r/with-let [loading? (rf/subscribe [:chapter/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               chapter (rf/subscribe [:chapter/details])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:div
          [page-header (if-not (empty? (:chapter_title @chapter))
                         (:chapter_title @chapter)
                         (:chapter_number @chapter))]
          [record-buttons]
          [:h3 {:style {:text-align "center"}} "Chapter Details"]
          (when @logged-in?
            [:div])
          (when @chapter
            [metadata-table (details->metadata @chapter :chapter)])
          (when @chapter
            [:div
             [:br]
             [:h3 {:style {:text-align "center"}} "Chapter Text"]
             [chapter-text-block (:chapter_html @chapter)]])])])))


(defn create-a-chapter
  "View for adding a new chapter to an existing title in the database."
  []
  (r/with-let [title-details (rf/subscribe [:title/details])]
    (fn []
      [:section.section>div.container>div.content
       [:div
        [:h1 {:style {:text-align "center"}} "Add A Chapter"]
        [adding-to-title @title-details]
        [new-chapter-form]]])))