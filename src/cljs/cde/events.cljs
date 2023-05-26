(ns cde.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

;; Navigation Dispatchers

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (println "Navigating: " match)
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :common/navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))




;; 'Docs' Page Dispatchers (fetching content for docs page, landing page, etc)

(rf/reg-event-db
 :set-landing-page
 (fn [db [_ docs]]
   (assoc db :landing-page docs)))

(rf/reg-event-fx
 :fetch-landing-page-text
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/landingtxt"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:set-landing-page]}}))





(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-landing-page]}))



(rf/reg-event-db
 :auth/handle-login
 (fn [db [_ {:keys [identity]}]]
   (assoc db :auth/user identity)))

(rf/reg-event-db
 :auth/handle-logout
 (fn [db _]
   (dissoc db :auth/user)))



(rf/reg-event-db
 :app/show-modal
 (fn [db [_ modal-id]]
   (assoc-in db [:app/active-modals modal-id] true)))

(rf/reg-event-db
 :app/hide-modal
 (fn [db [_ modal-id]]
   (update db :app/active-modals dissoc modal-id)))


;; SEARCH

(rf/reg-event-db
 :search/update-query
 (fn [db [_ field value]]
   (-> db
       (assoc-in [:search/query field] value)
       (assoc-in [:common/route :query-params field] value)
       (assoc-in [:common/route :parameters :query field] value))))

(rf/reg-event-db
 :search/clear-search-results
 (fn [db _]
   (-> db
       (assoc :search/results nil)
       (assoc :search/type nil)
       (assoc :search/time-loaded nil)
       (assoc :search/time-dispatched nil))))

(rf/reg-event-db
 :search/clear-search-query
 (fn [db _]
   (-> db
       (assoc :search/query {})
       (assoc :search/type nil)
       (assoc-in [:common/route :query-params] {}))))

(rf/reg-event-fx
 :search/submit-titles-search
 (fn [{:keys [db]} [_]]
   (let [search-query (-> db :common/route :query-params)]
     {:db (-> db
              (assoc :search/loading? true)
              (assoc :search/time-dispatched (js/Date.now)))
      :http-xhrio {:method          :get
                   :uri             "/api/search/titles"
                   :params          search-query
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:search/process-search-results]
                   :on-failure      [:search/process-search-error]}})))

(rf/reg-event-fx
 :search/submit-chapters-search
 (fn [{:keys [db]} [_]]
   (let [search-query (-> db :common/route :query-params)]
     {:db (-> db
              (assoc :search/time-dispatched (js/Date.now))
              (assoc :search/loading? true))
      :http-xhrio {:method          :get
                   :uri             "/api/search/chapters"
                   :params          search-query
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:search/process-search-results]
                   :on-failure      [:search/process-search-error]}})))

(rf/reg-event-db
 :search/process-search-results
 (fn [db [_ response]]
   (-> db
       (assoc :search/loading? false)
       (assoc :search/results (:results response))
       (assoc :search/type (:search_type response))
       (assoc :search/time-loaded (js/Date.now)))))

(rf/reg-event-db
 :search/process-search-error
 (fn [db [_ response]]
   (-> db
       (assoc :search/loading? false)
       (assoc :search/error (:message response)))))


;; VIEWING A USER PUBLIC PROFILE

(rf/reg-event-fx
 :profile/request-profile
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :profile/loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/user/" id "/profile")
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:profile/profile-loaded]
                   :on-failure      [:profile/profile-load-failed]}})))

(rf/reg-event-db
 :profile/profile-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :profile/loading? false)
       (assoc :profile/details response))))

(rf/reg-event-db
 :profile/profile-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :profile/loading? false)
       (assoc :profile/error (:message response)))))

(rf/reg-event-db
 :profile/clear-profile
 ;; remove :profile/loading? :profile/error and :profile/details from db
 (fn [db _]
   (-> db
       (dissoc :profile/loading?)
       (dissoc :profile/error)
       (dissoc :profile/details))))


;; VIEWING A NEWSPAPER
(rf/reg-event-fx
 :newspaper/request-newspaper
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :newspaper/loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/newspaper/" id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:newspaper/newspaper-loaded]
                   :on-failure      [:newspaper/newspaper-load-failed]}})))

(rf/reg-event-db
 :newspaper/newspaper-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/loading? false)
       (assoc :newspaper/details response))))

(rf/reg-event-db
 :newspaper/newspaper-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/loading? false)
       (assoc :newspaper/error (:message response)))))

(rf/reg-event-db
 :newspaper/clear-newspaper
 ;; remove :newspaper/loading? :newspaper/error and :newspaper/details from db
 (fn [db _]
   (-> db
       (dissoc :newspaper/loading?)
       (dissoc :newspaper/error)
       (dissoc :newspaper/details))))

;; VIEWING AN AUTHOR
(rf/reg-event-fx
 :author/request-author-metadata
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :author/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/author/" id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:author/author-loaded]
                   :on-failure      [:author/author-load-failed]}})))

(rf/reg-event-db
 :author/author-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :author/metadata-loading? false)
       (assoc :author/details response))))

(rf/reg-event-db
 :author/author-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :author/metadata-loading? false)
       (assoc :author/error (:message response)))))

(rf/reg-event-db
 :author/clear-author
 ;; remove :author/loading? :author/error and :author/details from db
 (fn [db _]
   (-> db
       (dissoc :author/metadata-loading?)
       (dissoc :author/titles-loading?)
       (dissoc :author/titles-error)
       (dissoc :author/error)
       (dissoc :author/details)
       (dissoc :author/titles))))

(rf/reg-event-fx
 :author/request-titles-by-author
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :author/titles-loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/author/" id "/titles")
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:author/author-titles-loaded]
                   :on-failure      [:author/author-titles-load-failed]}})))

(rf/reg-event-db
 :author/author-titles-load-failed
   (fn [db [_ response]]
    (-> db
        (assoc :author/titles-loading? false)
        (assoc :author/titles-error (:message response)))))

(rf/reg-event-db
 :author/author-titles-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :author/titles-loading? false)
       (assoc :author/titles response))))








;; VIEWING A TITLE
(rf/reg-event-fx
 :title/request-title
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :title/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/title/" id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:title/title-loaded]
                   :on-failure      [:title/title-load-failed]}})))

(rf/reg-event-db
 :title/title-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :title/metadata-loading? false)
       (assoc :title/details response))))

(rf/reg-event-db
 :title/title-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :title/metadata-loading? false)
       (assoc :title/error (:message response)))))

(rf/reg-event-db
 :title/clear-title
 ;; remove :title/loading? :title/error and :title/details from db
 (fn [db _]
   (-> db
       (dissoc :title/metadata-loading?)
       (dissoc :title/chapters-loading?)
       (dissoc :title/error)
       (dissoc :title/details)
       (dissoc :title/chapters))))

(rf/reg-event-fx
 :title/request-chapters-in-title
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :title/chapters-loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/title/" id "/chapters")
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:title/title-chapters-loaded]
                   :on-failure      [:title/title-chapters-load-failed]}})))
 
(rf/reg-event-db
 :title/title-chapters-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :title/chapters-loading? false)
       (assoc :title/chapters response))))

(rf/reg-event-db
  :title/title-chapters-load-failed
  (fn [db [_ response]]
    (-> db
        (assoc :title/chapters-loading? false)
        (assoc :title/chapters-error (:message response)))))










;; VIEWING A CHAPTER
(rf/reg-event-fx
 :chapter/request-chapter
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :chapter/loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/chapter/" id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:chapter/chapter-loaded]
                   :on-failure      [:chapter/chapter-load-failed]}})))

(rf/reg-event-db
 :chapter/chapter-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/loading? false)
       (assoc :chapter/details response))))

(rf/reg-event-db
 :chapter/chapter-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/loading? false)
       (assoc :chapter/error (:message response)))))

(rf/reg-event-db
 :chapter/clear-chapter
 ;; remove :chapter/loading? :chapter/error and :chapter/details from db
 (fn [db _]
   (-> db
       (dissoc :chapter/loading?)
       (dissoc :chapter/error)
       (dissoc :chapter/details))))



;; ADDING NEW NEWSPAPER/STORY/CHAPTER RECORDS

;; frontend db events (forms while they're being filled out)
(rf/reg-event-db
 :newspaper/update-new-newspaper-form-field
 (fn [db [_ field value]]
   (assoc-in db [:newspaper/new-newspaper-form field] value)))




;; GETTING COUNTS OF RECORDS (total n chapters, n stories, n newspapers)
(rf/reg-event-fx
 ;; event for dispatching the http request to the api to 'get stats' about the platform
 :platform/get-statistics
 (fn [{:keys [db]} [_]]
   {:db (assoc db :platform/statistics-loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/platform/statistics"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:platform/statistics-loaded]
                 :on-failure      [:platform/statistics-load-failed]}}))

(rf/reg-event-db
 ;; event for updating the db with the stats from the api
 :platform/statistics-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :platform/statistics-loading? false)
       (assoc :platform/statistics response))))

(rf/reg-event-db
  ;; event for updating the db when an attempt to get stats from the api fails
 :platform/statistics-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :platform/statistics-loading? false)
       (assoc :platform/statistics-error (:message response)))))

;; GETTING SEARCH OPTIONS (eg author nationalities, genders)

(rf/reg-event-fx
 ;; event for dispatching the http request to the api to 'get search options' about the platform
 :platform/get-search-options
 (fn [{:keys [db]} [_]]
   {:db (assoc-in db [:platform/search-options :loading?] true)
    :http-xhrio {:method          :get
                 :uri             "/api/platform/search-options"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:platform/search-options-loaded]
                 :on-failure      [:platform/search-options-load-failed]}}))

(rf/reg-event-db
  ;; event for updating the db with the search options from the api
  :platform/search-options-loaded
  (fn [db [_ response]]
    (-> db
        (assoc-in [:platform/search-options :loading?] false)
        (assoc :platform/search-options response))))

(rf/reg-event-db
  ;; event for updating the db when an attempt to get search options from the api fails
  :platform/search-options-load-failed
  (fn [db [_ response]]
    (-> db
        (assoc-in [:platform/search-options :loading?] false)
        (assoc-in [:platform/search-options :error] (:message response)))))