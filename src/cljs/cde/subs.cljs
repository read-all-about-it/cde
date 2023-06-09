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
 :common/error
 (fn [db _]
   (:common/error db)))



;; AUTHENTICATION INFO

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
 :auth/username
 :<- [:auth/user]
 (fn [user _]
   (-> user :username)))


;; APP MODALS

(rf/reg-sub
 :app/active-modals
 (fn [db _]
   (:app/active-modals db {})))

(rf/reg-sub
 :app/modal-showing?
 :<- [:app/active-modals]
 (fn [modals [_ modal-id]]
   (get modals modal-id false)))




;; SEARCH

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
   (get-in db [:search/results] [])))

(rf/reg-sub
 :search/time-dispatched
 (fn [db _]
   (get db :search/time-dispatched nil)))

(rf/reg-sub
 :search/time-loaded
 (fn [db _]
   (get db :search/time-loaded nil)))

(rf/reg-sub
 :search/type
 (fn [db _]
   (get db :search/type nil)))

(rf/reg-sub
 :search/error
 (fn [db _]
   (get db :search/error nil)))


;; PUBLIC USER PROFILES
(rf/reg-sub
 :profile/loading?
 (fn [db _]
   (get db :profile/loading? true)))

(rf/reg-sub
 :profile/details
 (fn [db _]
   (get db :profile/details {})))

(rf/reg-sub
 :profile/name
 :<- [:profile/details]
 (fn [details _]
   (-> details :name)))


;; VIEWING A NEWSPAPER
(rf/reg-sub
 :newspaper/metadata-loading?
 (fn [db _]
   (get db :newspaper/metadata-loading? true)))

(rf/reg-sub
 :newspaper/titles-loading?
 (fn [db _]
   (get db :newspaper/titles-loading? true)))

(rf/reg-sub
 :newspaper/details
 (fn [db _]
   (get db :newspaper/details {})))

(rf/reg-sub
 :newspaper/titles
 (fn [db _]
   (get db :newspaper/titles [])))

;; VIEWING AN AUTHOR

(rf/reg-sub
 :author/metadata-loading?
 (fn [db _]
   (get db :author/metadata-loading? true)))

(rf/reg-sub
 :author/titles-loading?
 (fn [db _]
   (get db :author/titles-loading? true)))

(rf/reg-sub
 :author/details
 (fn [db _]
   (get db :author/details {})))

(rf/reg-sub
 :author/titles
 (fn [db _]
   (get db :author/titles [])))


;; VIEWING A TITLE

(rf/reg-sub
 :title/metadata-loading?
 (fn [db _]
   (get db :title/metadata-loading? true)))

(rf/reg-sub
 :title/chapters-loading?
 (fn [db _]
   (get db :title/chapters-loading? true)))

(rf/reg-sub
 :title/details
 (fn [db _]
   (get db :title/details {})))

(rf/reg-sub
 :title/chapters
 (fn [db _]
   (get db :title/chapters [])))





;; VIEWING A CHAPTER

(rf/reg-sub
 :chapter/loading?
 (fn [db _]
   (get db :chapter/loading? true)))

(rf/reg-sub
 :chapter/details
 (fn [db _]
   (get db :chapter/details {})))

;; ADDING NEW RECORDS

;; frontend form data
(rf/reg-sub
 :newspaper/new-newspaper-form
 (fn [db _]
   (get db :newspaper/new-newspaper-form {})))




;; PLATFORM STATISTICS (counts of newspaper/title/chapter records)

(rf/reg-sub
 :platform/statistics
 (fn [db _]
   (get db :platform/statistics {})))

(rf/reg-sub
 :platform/statistics-loading?
 (fn [db _]
   (get db :platform/statistics-loading? true)))

(rf/reg-sub
 :platform/statistics-error
 (fn [db _]
   (get db :platform/statistics-error nil)))

(rf/reg-sub
 :platform/newspaper-count
 :<- [:platform/statistics]
 (fn [stats _]
   (-> stats :newspaper-count)))

(rf/reg-sub
 :platform/title-count
 :<- [:platform/statistics]
 (fn [stats _]
   (-> stats :title-count)))

(rf/reg-sub
 :platform/chapter-count
 :<- [:platform/statistics]
 (fn [stats _]
   (-> stats :chapter-count)))

(rf/reg-sub
 :platform/author-count
 :<- [:platform/statistics]
 (fn [stats _]
   (-> stats :author-count)))

(rf/reg-sub
 :platform/search-options
 (fn [db _]
   (get db :platform/search-options {})))

(rf/reg-sub
 :platform/search-options-loading?
 (fn [db _]
   (get-in db [:platform/search-options :loading?] true)))

(rf/reg-sub
  :platform/search-options-error
  (fn [db _]
    (get-in db [:platform/search-options :error] nil)))

(rf/reg-sub
 :platform/author-nationalities
 :<- [:platform/search-options]
 (fn [options _]
   (-> options :author-nationalities)))

(rf/reg-sub
 :platform/author-genders
 :<- [:platform/search-options]
 (fn [options _]
   (-> options :author-genders)))



;; Static Page Text (ie, landing page, docs, etc)

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :landing-page
 (fn [db _]
   (:landing-page db)))