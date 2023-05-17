(ns cde.pages.profile
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]))


(defn profile-page []
  (r/with-let [loading? (rf/subscribe [:profile/loading?])
               username (rf/subscribe [:auth/username])
               profile-name (rf/subscribe [:profile/name])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:h1 {:style {:text-align "center"}} @profile-name])])))