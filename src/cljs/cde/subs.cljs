(ns cde.subs 
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

(rf/reg-sub
 :common/page-id
 :<- [:common/route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   (-> route :data :view)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))



(rf/reg-sub
 :auth/user
 (fn [db _]
   (:auth/user db)))

(rf/reg-sub
 :auth/logged-in?
 :<- [:auth/user]
 (fn [user _]
   (not (nil? user))))

(rf/reg-sub
 :app/active-modals
 (fn [db _]
   (:app/active-modals db {})))

(rf/reg-sub
 :app/modal-showing?
 :<- [:app/active-modals]
 (fn [modals [_ modal-id]]
   (get modals modal-id false)))





(rf/reg-sub
 :search/query
 (fn [db _]
   (get db :search/query {})))

(rf/reg-sub
 :search/loading?
 (fn [db _]
   (get db :search/loading? false)))

(rf/reg-sub
 :search/results
 (fn [db _]
   (get db :search/results [])))