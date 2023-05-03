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
      [:div.field
       [:div.control 
        [:input.input
         {:type "text"
               :placeholder "Search by common title..."
               :value @query
               :on-change #(rf/dispatch [:search/update-query (-> % .-target .-value)])}]]])))