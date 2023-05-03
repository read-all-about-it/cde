(ns cde.components.forms
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]))


(defn search-input []
  (r/with-let [query (rf/subscribe [:search/query])
               error (r/atom nil)]
    (fn []
      [:div
       [:div.field.is-horizontal
        [:div.field-body
         [:div.field
          [:div.control
           [:input.input
            {:type "text"
             :placeholder "Search by common title..."
             :value (:common-title @query)
             :on-change #(rf/dispatch [:search/update-query :common-title (-> % .-target .-value)])}]]]
         [:div.field
          [:div.control
           [:input.input
            {:type "text"
             :placeholder "Search by newspaper title..."
             :value (:newspaper-title @query)
             :on-change #(rf/dispatch [:search/update-query :newspaper-title (-> % .-target .-value)])}]]]]]
       [:div.field.is-horizontal
        [:div.field-body
         [:div.field.is-expanded
          [:div.control
           [:input.input
            {:type "text"
             :placeholder "Search by author..."
             :value (:author @query)
             :on-change #(rf/dispatch [:search/update-query :author (-> % .-target .-value)])}]]]
         [:div.field
          [:div.control
           [:div.select
            [:select
             {:value (:nationality @query)
              :on-change #(rf/dispatch [:search/update-query :nationality (-> % .-target .-value)])}
             [:option {:value "" :disabled true :selected true :style {:color "gray"}} "Author nationality"]
             ; Add more options for each nationality
             [:option {:value "Australian"} "Australian"]
             [:option {:value "British"} "British"]]]]]
         [:div.field
          [:div.control
           [:div.select
            [:select
             {:value (:gender @query)
              :on-change #(rf/dispatch [:search/update-query :gender (-> % .-target .-value)])}
             [:option {:value "" :disabled true :selected true :style {:color "gray"}} "Author gender"]
             ; Add more options for each gender
             [:option {:value "Male"} "Male"]
             [:option {:value "Female"} "Female"]]]]]]]])))