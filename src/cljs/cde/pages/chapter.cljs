(ns cde.pages.chapter
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table]]
   [cde.utils :refer [details->metadata]]))



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
          [:h1 {:style {:text-align "center"}} (:chapter_title @chapter)]
          [:h3 {:style {:text-align "center"}} "(Chapter Details)"]
          (when @logged-in?
            [:div])
          (when @chapter
            [metadata-table (details->metadata @chapter :chapter)])
          (when @chapter
            [:div
             [:br]
             [:h3 {:style {:text-align "center"}} "Chapter Text"]
             [chapter-text-block (:chapter_html @chapter)]])
          ])])))