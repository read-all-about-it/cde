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


;; Auth0 Subs

(rf/reg-sub
 :auth0-client
 (fn [db _]
   (:auth0-client db)))



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
   (get db :search/loading?)))

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
   (get db :profile/loading?)))

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
   (get db :newspaper/metadata-loading?)))

(rf/reg-sub
 :newspaper/titles-loading?
 (fn [db _]
   (get db :newspaper/titles-loading?)))

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
   (get db :author/metadata-loading?)))

(rf/reg-sub
 :author/titles-loading?
 (fn [db _]
   (get db :author/titles-loading?)))

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
   (get db :title/metadata-loading?)))

(rf/reg-sub
 :title/chapters-loading?
 (fn [db _]
   (get db :title/chapters-loading?)))

(rf/reg-sub
 :title/error
 (fn [db _]
   (get db :title/error nil)))

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

(rf/reg-sub
 :author/new-author-form 
 (fn [db _]
   (get db :author/new-author-form {})))

(rf/reg-sub
 :title/new-title-form
 (fn [db _]
   (get db :title/new-title-form {})))

(rf/reg-sub
 :chapter/new-chapter-form
 (fn [db _]
   (get db :chapter/new-chapter-form {})))



;; UPDATING/EDITING EXISTING RECORDS
(rf/reg-sub
 :newspaper/edit-newspaper-form
 (fn [db _]
   (get db :newspaper/edit-newspaper-form {})))

(rf/reg-sub
 :author/edit-author-form
 (fn [db _]
   (get db :author/edit-author-form {})))

(rf/reg-sub
 :title/edit-title-form
 (fn [db _]
   (get db :title/edit-title-form {})))

(rf/reg-sub
 :chapter/edit-chapter-form
 (fn [db _]
   (get db :chapter/edit-chapter-form {})))











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
 :platform/about-page-text
 (fn [db _]
   (get-in db [:static-content :about])))

(rf/reg-sub
 :platform/team-page-text
 (fn [db _]
   (get-in db [:static-content :team])))

(rf/reg-sub
 :platform/faq-page-text
 (fn [db _]
   (get-in db [:static-content :faq])))



;; TROVE API

(rf/reg-sub
 ;; details of the most recently fetched record from Trove (could be a newspaper or chapter)
 :trove/details ;; usage: (rf/subscribe [:trove/details])
 (fn [db _]
   (get db :trove/details {})))

(rf/reg-sub
 ;; the list of chapter records we've fetched from Trove and stored in the user db
 :trove/chapters ;; usage: (rf/subscribe [:trove/chapters])
 (fn [db _]
   (get-in db [:trove/records :chapters] [])))

(rf/reg-sub
 ;; the list of newspaper records we've fetched from Trove and stored in the user db
 :trove/newspapers ;; usage: (rf/subscribe [:trove/newspapers]) 
 (fn [db _]
   (get-in db [:trove/records :newspapers] [])))

(rf/reg-sub
 ;; whether we're currently fetching a record from Trove
 :trove/loading? ;; usage: (rf/subscribe [:trove/loading?])
  (fn [db _]
    (get-in db [:trove/loading?] false)))

(rf/reg-sub
 ;; any error that occurred while fetching a record from Trove (newspaper or chapter)
 :trove/error
 (fn [db _]
   (get-in db [:trove/error] nil)))


(rf/reg-sub
 :tbc/titles ;; usage: (rf/subscribe [:tbc/titles])
 (fn [db _]
   (get-in db [:tbc/records :titles] [])))





(rf/reg-sub
 :trove/chapter-exists-list
 (fn [db _]
   (get-in db [:trove/ids-already-in-db :chapters] [])))









(rf/reg-sub
 :chapter/creation-loading?
 (fn [db _]
   (get-in db [:chapter/creating?] nil)))

(rf/reg-sub
 :chapter/creation-error
  (fn [db _]
    (get-in db [:chapter/creation-error] nil)))

(rf/reg-sub
 :chapter/creation-success
  (fn [db _]
    (get-in db [:chapter/creation-success] nil)))

(rf/reg-sub
 :chapter/update-loading?
 (fn [db _]
   (get-in db [:chapter/updating?] nil)))

(rf/reg-sub
 :chapter/update-error
  (fn [db _]
    (get-in db [:chapter/update-error] nil)))

(rf/reg-sub
  :chapter/update-success
    (fn [db _]
      (get-in db [:chapter/update-success] nil)))



(rf/reg-sub
 :title/creation-loading?
 (fn [db _]
   (get-in db [:title/creating?] nil)))

(rf/reg-sub
  :title/creation-error
    (fn [db _]
      (get-in db [:title/creation-error] nil)))

(rf/reg-sub
  :title/creation-success
    (fn [db _]
      (get-in db [:title/creation-success] nil)))

(rf/reg-sub
  :title/update-loading?
    (fn [db _]
      (get-in db [:title/updating?] nil)))

(rf/reg-sub
  :title/update-error
    (fn [db _]
      (get-in db [:title/update-error] nil)))

(rf/reg-sub
  :title/update-success
    (fn [db _]
      (get-in db [:title/update-success] nil)))