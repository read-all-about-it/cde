(ns cde.pages.profile
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]))


(defn profile-page []
  (let [match (rf/subscribe [:common/page])]
    (fn []
      (let [params (:params @match)
            id (:id params)]
        [:div
         [:h1 "Profile Page"]
         [:p "User ID: " id]]))))