(ns cde.events
  (:require
    [re-frame.core :as rf]
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
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))


(rf/reg-event-fx
  :page/init-home
  (fn [_ _]
    {:dispatch [:fetch-docs]}))



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
   (assoc-in db [:search/query field] value)))

(rf/reg-event-db
 :search/clear-results
 (fn [db _]
   (assoc db :search/results nil)))


(rf/reg-event-fx
  :search/submit-search
  (fn [{:keys [db]} [_ search-query]]
    (merge
      {:db (assoc db :search/loading? true)}
      {:http-xhrio {:method          :get
                    :uri             "/search"
                    :params          search-query
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:search/process-search-results]
                    :on-failure      [:search/process-search-error]}})))

(rf/reg-event-db
  :search/process-search-results
  (fn [db [_ response]]
    (-> db
        (assoc :search/loading? false)
        (assoc :search/results (:results response)))))

(rf/reg-event-db
  :search/process-search-error
  (fn [db [_ response]]
    (-> db
        (assoc :search/loading? false)
        (assoc :search/error (:message response)))))


;; USER PUBLIC PROFILE

(rf/reg-event-fx
 :profile/request-profile
 (fn [{:keys [db]} [_]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :profile/loading? true)
      :http-xhrio {:method          :get
                   :uri             (str "/api/profile/" id)
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



;; ADDING NEW NEWSPAPER/STORY/CHAPTER RECORDS

;; frontend db events (forms while they're being filled out)
(rf/reg-event-db
 :newspaper/update-new-newspaper-form-field
 (fn [db [_ field value]]
   (assoc-in db [:newspaper/new-newspaper-form field] value)))