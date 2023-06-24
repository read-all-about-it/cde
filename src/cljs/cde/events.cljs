(ns cde.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [cde.utils :refer [endpoint]]))

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
                   :uri             (endpoint "search" "titles")
                   :params          search-query
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:search/process-search-results]
                   :on-failure      [:search/process-search-error]}})))

(rf/reg-event-fx
 :search/submit-chapter-text-search
 (fn [{:keys [db]} [_]]
   (let [search-query (-> db :common/route :query-params)]
     {:db (-> db
              (assoc :search/time-dispatched (js/Date.now))
              (assoc :search/loading? true))
      :http-xhrio {:method          :get
                   :uri             (endpoint "search" "chapters")
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




;; 
(rf/reg-event-fx
 :profile/request-profile
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :profile/loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "user" id "profile")
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
 :newspaper/request-newspaper-metadata
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :newspaper/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "newspaper" id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:newspaper/newspaper-loaded]
                   :on-failure      [:newspaper/newspaper-load-failed]}})))

(rf/reg-event-db
 :newspaper/newspaper-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/metadata-loading? false)
       (assoc :newspaper/details response))))

(rf/reg-event-db
 :newspaper/newspaper-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/metadata-loading? false)
       (assoc :newspaper/error (:message response)))))

(rf/reg-event-db
 :newspaper/clear-newspaper
 ;; remove :newspaper/metadata-loading? :newspaper/error and :newspaper/details from db
 (fn [db _]
   (-> db
       (dissoc :newspaper/metadata-loading?)
       (dissoc :newspaper/error)
       (dissoc :newspaper/details))))

(rf/reg-event-fx
 :newspaper/request-titles-in-newspaper
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :newspaper/titles-loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "newspaper" id "titles")
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:newspaper/newspaper-titles-loaded]
                   :on-failure      [:newspaper/newspaper-titles-load-failed]}})))

(rf/reg-event-db
 :newspaper/newspaper-titles-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/titles-loading? false)
       (assoc :newspaper/titles-error (:message response)))))

(rf/reg-event-db
 :newspaper/newspaper-titles-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/titles-loading? false)
       (assoc :newspaper/titles response))))

;; VIEWING AN AUTHOR
(rf/reg-event-fx
 :author/request-author-metadata
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :author/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "author" id)
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
                   :uri             (endpoint "author" id "titles")
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


;; --- GET Title (metadata) @ /title/:id ---------------------------------------

(rf/reg-event-fx
 :title/get-title
 (fn [{:keys [db]} [_ pos-id]]
   (let [path-id (-> db :common/route :path-params :id)]
     {:db (assoc db :title/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "title" (if path-id path-id pos-id))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:title/title-loaded]
                   :on-failure      [:title/title-load-failed]}})))

(rf/reg-event-db
 :title/title-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :title/metadata-loading? false)
       (assoc :title/details response)
       (assoc :title/error nil)
       (update-in [:tbc/records :titles] conj response)
       (update-in [:tbc/records :titles] distinct))))

(rf/reg-event-db
 :title/title-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :title/metadata-loading? false)
       (assoc :title/error (or (:message response)
                               (get-in response [:response :responseText])
                               "Unknown error")))))

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
                   :uri             (endpoint "title" id "chapters")
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

;; --- GET Chapter @ /api/v1/chapter/:id --------------------------------------
(rf/reg-event-fx
 :chapter/get-chapter
 (fn [{:keys [db]} [_ pos-id]]
   (let [path-id (-> db :common/route :path-params :id)]
     {:db (assoc db :chapter/loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "chapter" (if pos-id pos-id path-id))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:chapter/chapter-loaded]
                   :on-failure      [:chapter/chapter-load-failed]}})))

(rf/reg-event-db
 :chapter/chapter-loaded
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/loading? false)
       (assoc :chapter/details response)
       (update-in [:tbc/records :chapters] conj response) ;; add the chapter record
       (update-in [:tbc/records :chapters] distinct) ;; remove duplicates
       )))

(rf/reg-event-db
 :chapter/chapter-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/loading? false)
       (assoc :chapter/error (or (:message response)
                                 (get-in response [:response :message])
                                 "Unknown error")))))

(rf/reg-event-db
 :chapter/clear-chapter
 ;; remove :chapter/loading? :chapter/error and :chapter/details from db (but keep in :tbc/records)
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

(rf/reg-event-db
 :title/update-new-title-form-field
 (fn [db [_ field value]]
   (assoc-in db [:title/new-title-form field] value)))

(rf/reg-event-db
 :chapter/update-new-chapter-form-field
 (fn [db [_ field value]]
   (assoc-in db [:chapter/new-chapter-form field] value)))


;; prepopulate 'new-chapter-form' fields with relevant data from :trove/details
(rf/reg-event-db
 :chapter/populate-new-chapter-form
 (fn [db [_]]
   (let [trove-details (-> db
                           (get-in [:trove/details] {})
                           (dissoc :trove_newspaper_url :trove_article_id))]
     (update-in db [:chapter/new-chapter-form] merge trove-details))))





;; --- POST Chapter @ /api/v1/create/chapter ----------------------------------




















;; GETTING COUNTS OF RECORDS (total n chapters, n stories, n newspapers)
(rf/reg-event-fx
 ;; event for dispatching the http request to the api to 'get stats' about the platform
 :platform/get-statistics
 (fn [{:keys [db]} [_]]
   {:db (assoc db :platform/statistics-loading? true)
    :http-xhrio {:method          :get
                 :uri             (endpoint "platform" "statistics")
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
                 :uri             (endpoint "platform" "search-options")
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

;; GETTING CREATION FORM FIELD OPTIONS (eg existing newspapers to associate a new title with)
(rf/reg-event-fx
 ;; event for dispatching the http request to the api to 'get platform-wide creation form field options')
  :platform/get-creation-form-options
 (fn [{:keys [db]} [_]]
   {:db (assoc-in db [:platform/creation-form-field-options :loading?] true)
    :http-xhrio {:method          :get
                 :uri             (endpoint "platform" "creation-options")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:platform/creation-form-options-loaded]
                 :on-failure      [:platform/creation-form-options-load-failed]}}))

(rf/reg-event-db
  ;; event for updating the db with the form field options from the api
  :platform/creation-form-options-loaded
  (fn [db [_ response]]
    (-> db
        (assoc-in [:platform/creation-form-field-options :loading?] false)
        (assoc :platform/creation-form-field-options response))))

(rf/reg-event-db
  ;; event for updating the db when an attempt to get form field options from the api fails
  :platform/creation-form-options-load-failed
  (fn [db [_ response]]
    (-> db
        (assoc-in [:platform/creation-form-field-options :loading?] false)
        (assoc-in [:platform/creation-form-field-options :error] (:message response)))))




;; EVENT HANDLERS FOR GETTING RECORDS FROM TROVE (via *our* API)

;; --- GET Trove Chapter @ /api/v1/trove/chapter/:trove_article_id ------------
(rf/reg-event-fx
 :trove/get-chapter
  (fn [{:keys [db]} [_ trove-article-id]]
    {:db (assoc db :trove/loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "trove" "chapter" trove-article-id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:trove/chapter-loaded]
                   :on-failure      [:trove/chapter-load-failed]}}))

(rf/reg-event-db
 :trove/chapter-loaded ;; append the chapter to the db at [:trove/records :chapters], and replace whatever is in :trove/details
 (fn [db [_ response]]
   (-> db
       (assoc :trove/loading? false)
       (assoc :trove/error nil)
       (assoc :trove/details response)
       (update-in [:trove/records :chapters] conj response)
       (update-in [:trove/records :chapters] distinct))))

(rf/reg-event-db
  :trove/chapter-load-failed
  (fn [db [_ response]]
    (-> db
        (assoc :trove/loading? false)
        (assoc :trove/error (:message (:response response))))))

;; --- GET Trove Newspaper @ /api/v1/trove/newspaper/:trove_newspaper_id ------
(rf/reg-event-fx
 :trove/get-newspaper 
 (fn [{:keys [db]} [_ trove-newspaper-id]]
   {:db (assoc db :trove/loading? true)
    :http-xhrio {:method          :get
                 :uri             (endpoint "trove" "newspaper" trove-newspaper-id)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:trove/newspaper-loaded]
                 :on-failure      [:trove/newspaper-load-failed]}}))

(rf/reg-event-db
 :trove/newspaper-loaded ;; append the newspaper to the db at [:trove/records :newspapers], and replace whatever is in :trove/details
 (fn [db [_ response]]
   (-> db
       (assoc :trove/loading? false) ;; no longer waiting on content from trove
       (assoc :trove/error nil)
       (assoc :trove/details response)
       (update-in [:trove/records :newspapers] conj response) ;; append the newspaper
       (update-in [:trove/records :newspapers] distinct)))) ;; remove any duplicates

(rf/reg-event-db
  :trove/newspaper-load-failed
  (fn [db [_ response]]
    (-> db
        (assoc :trove/loading? false)
        (assoc :trove/error (:message response)))))




;; EVENT HANDLERS FOR CHECKING WHETHER A RECORD IS ALREADY IN OUR DATABASE (GIVEN ITS TROVE ID)

;; --- GET Chapter Exists? @ /api/v1/trove/exists/chapter/:trove_article_id ------
(rf/reg-event-fx
 :trove/get-chapter-exists
  (fn [{:keys [db]} [_ trove-article-id]]
    {:db (assoc db :trove/loading? true)
      :http-xhrio {:method          :get
                  :uri             (endpoint "trove" "exists" "chapter" trove-article-id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:trove/chapter-exists-loaded]
                  :on-failure      [:trove/chapter-exists-load-failed]}}))

(rf/reg-event-db
  :trove/chapter-exists-loaded
  (fn [db [_ response]]
    (-> db
        (assoc :trove/loading? false)
        (assoc :trove/error nil)
        (assoc :trove/exists? response))))
