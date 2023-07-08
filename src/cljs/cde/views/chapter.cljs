(ns cde.views.chapter
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.login :refer [auth0-login-to-edit-button]]
   [cde.components.metadata :refer [metadata-table
                                    adding-to-title]]
   [cde.utils :refer [details->metadata]]
   [cde.components.forms :refer [new-chapter-form]]
   [cde.components.editing-records :refer [edit-chapter-form]]
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
               error (rf/subscribe [:chapter/error])]
    (fn []
      [:section.section>div.container>div.content
       (when (and (not @error) (not @loading?) (not @chapter))
         (rf/dispatch [:chapter/get-chapter]))
       
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
  "View for editing an existing chapter in the database."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])
               chapter-details (rf/subscribe [:chapter/details])]
    (fn []
      [:section.section>div.container>div.content
       (cond
         (:chapter_title @chapter-details) [page-header "Edit A Chapter"
                                            [:a {:href (str "#/chapter/" (:id @chapter-details))}
                                             (:chapter_title @chapter-details)]]
         :else [page-header "Edit A Chapter"])
       (if @logged-in?
         [edit-chapter-form]
         [auth0-login-to-edit-button])])))