(ns cde.pages.create
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as string]))


(defn add-a-newspaper-page []
  (r/with-let [details (rf/subscribe [:newspaper/new-newspaper-form])
               is-number (fn [x] (re-find #"^\d+$" x))]
  (fn []
    [:section.section>div.container>div.content
     [:h1 {:style {:text-align "center"}} "Add a New Newspaper to the Database"]
     [:div.field
      [:label.label "Trove Newspaper ID"]
      [:div.control.has-icons-left.has-icons-right
       [:input.input
        {:type "text"
         :placeholder "Newspaper ID..."
         :required true
         :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :trove-newspaper-id (-> % .-target .-value)])}]
       [:span.icon.is-small.is-left
        [:i.material-icons "newspaper"]]
       (when-not (is-number (:trove-newspaper-id @details))
         [:span.icon.is-small.is-right
          [:i.material-icons "error_outline"]])]]
     [:div.field
      [:label.label "Newspaper Title"]
      [:div.control
       [:input.input
        {:type "text"
         :placeholder "Newspaper Title..."}]]]])))


(defn add-a-title-page []
  (fn []
    [:section.section>div.container>div.content
     [:h1 {:style {:text-align "center"}} "Add a New Title to the Database"]]))

(defn add-a-chapter-page []
  (fn []
    [:section.section>div.container>div.content
     [:h1 {:style {:text-align "center"}} "Add a New Chapter to the Database"]]))

(defn add-an-author-page []
  (fn []
    [:section.section>div.container>div.content
     [:h1 {:style {:text-align "center"}} "Add a New Author to the Database"]]))