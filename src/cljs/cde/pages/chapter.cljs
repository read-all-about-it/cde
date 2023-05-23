(ns cde.pages.chapter
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-block]]))


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
          [:h1 {:style {:text-align "center"}} (:chapter_title @chapter)]
          [:h3 {:style {:text-align "center"}} "(Chapter Details)"]
          (when @logged-in?
            [:div])
          [metadata-block @chapter
           [:chapter_number
            :chapter_title
            :final_date
            :page_references
            :word_count
            :illustrated
            :article_url]
           {:chapter_number "Chapter Number"
            :chapter_title "Chapter Title"
            :final_date "Final Date"
            :page_references "Page Number"
            :word_count "Word Count"
            :illustrated "Illustrated"}
           ]])])))