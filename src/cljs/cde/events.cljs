(ns cde.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [cde.utils :refer [endpoint]]
   [clojure.string :as str]
   [cljs.reader :as reader]
   [cde.config :as config]
   ["@auth0/auth0-spa-js" :as auth0]))


;; -----------------------------------------------------------------------------
;; -------------------- LOCAL STORE MANAGEMENT ---------------------------------
;; -- Interceptors & events & cofx for handling local store user auth details --
;; -----------------------------------------------------------------------------

(def tbc-user-ls-key "to-be-continued-user") ;; localstore key for tbc user auth info

(defn set-auth-ls
  "Store auth information in local store. A sorted map written as an EDN string."
  [auth]
  (.setItem js/localStorage tbc-user-ls-key (str auth))) ;; written as EDN string

(defn remove-auth-ls
  "Remove auth information from local store when logging out."
  []
  (.removeItem js/localStorage tbc-user-ls-key))

(def set-auth-interceptor [(rf/path :auth) ;; the `:auth` path within the db (not the whole frontend db)
                           (rf/after set-auth-ls) ;; write user to local store (after)
                           rf/trim-v]) ;; remove the first (event id) element from the event vec

(def remove-auth-interceptor [(rf/after remove-auth-ls)])

(rf/reg-cofx
 :local-store-auth ;; the name of the coeffect
 (fn [cofx _]
   (assoc cofx :local-store-auth ;; put the local-store auth into the coeffect under :local-store-auth
          (into (sorted-map) ;; read in user from localstore & process into a sorted map
                (some->> (.getItem js/localStorage tbc-user-ls-key)
                         (reader/read-string)))))) ;; EDN map -> map


(rf/reg-event-fx                                         ;; usage: (dispatch [:auth/initialise])
 :auth/initialise                                        ;; gets called when the app starts; gets auth details from localstore and puts into db

 ;; the interceptor chain (a vector of interceptors)
 [(rf/inject-cofx :local-store-auth)]                    ;; gets auth details from localstore, and puts into coeffects arg

 ;; the event handler (function) being registered. Ignore the effect vector itself.
 (fn [{:keys [db local-store-auth]} _]
   (.log js/console "initialising auth" local-store-auth)
   {:db (assoc-in db [:auth] local-store-auth)           ;; put the auth details into the db under :auth
    :dispatch [:auth/create-auth0-client]}))             ;; dispatch another event to create the auth0 client

(rf/reg-event-fx
 :auth/set-auth-in-ls
 set-auth-interceptor
 (fn [{auth :db} [{props :auth}]]
   {:db (-> (merge auth props)
            (assoc-in [:last-saved] (js/Date.now)))}))

(rf/reg-event-fx
 :auth/remove-auth-from-ls
 remove-auth-interceptor
 (fn [{auth :db} _]
   {:db (dissoc auth)}))




;; ----------------------------------------------------------------------------
;; ------------------------- AUTH0 --------------------------------------------
;; ----------------------------------------------------------------------------

(rf/reg-event-fx
 ;; initialises an auth0 client and stores it in the app db under [:auth0-client]
 :auth/create-auth0-client
 (fn [{:keys [db]} _]
   (let [details {:clientId (get config/auth0-details :client-id)
                  :domain (get config/auth0-details :domain)
                  :scope (get config/auth0-details :scope)
                  :redirectUri (get config/auth0-details :redirect-uri)
                  :audience (get config/auth0-details :audience)
                  ;; :responseType "token"
                  :cacheLocation "localstorage"}
         client (auth0/Auth0Client. (clj->js details))]
     (.log js/console "Created Auth0 client:" client)
     (.log js/console "Details given to Auth0 client: " details)
     {:db (assoc-in db [:auth0-client] client)})))

(rf/reg-event-fx
 :auth/store-auth0-user-in-db
 (fn [{:keys [db]} [_ user]] ;; user here is the auth0 user object, which needs to be translated to a map
   (let [clean-user (js->clj user :keywordize-keys true)]
     {:db (assoc-in db [:auth :user] clean-user) ;; store the user in the app db
      :dispatch-n [[:auth/set-auth-in-ls]  ;; dispatch an event to store the current auth state in local storage
                   [:auth/get-user-id-from-db] ;; get the user id from *our* db
                   ]})))

(rf/reg-event-fx
 :auth/store-auth0-error-in-db
 (fn [{:keys [db]} [_ error]] ;; error here is the auth0 error object, which needs to be translated to a map
   (let [clean-error (js->clj error :keywordize-keys true)]
     {:db (assoc-in db [:auth :error] clean-error)})))

(rf/reg-event-fx
 :auth/remove-auth0-user-from-db
 (fn [{:keys [db]} _]
   {:db (dissoc db :auth)
    :dispatch [:auth/remove-auth-from-ls]}))

(rf/reg-event-fx
 :auth/login-auth0-with-popup
 (fn [{:keys [db]} _]
   (let [client (get db :auth0-client)
         options (clj->js {:scope (get config/auth0-details :scope)})] ;; define additional options if required
     (js/Promise. (fn [resolve reject]
                    (-> client
                        (.loginWithPopup options)
                        (.then (fn [_]
                                 (-> client
                                     (.getUser)
                                     (.then (fn [user]
                                              (.log js/console "User logged in: " user)
                                              (rf/dispatch [:auth/store-auth0-user-in-db user])
                                              (resolve user)))
                                     (.catch (fn [err]
                                               (.log js/console "Error getting user: " err)
                                               (rf/dispatch [:auth/store-auth0-error-in-db err])
                                               (reject err))))))
                        (.catch (fn [err]
                                  (.log js/console "Error during login: " err)
                                  (rf/dispatch [:auth/store-auth0-error-in-db err])
                                  (reject err))))))
     {:db db})))

(rf/reg-event-fx
 :auth/logout-auth0
 ;; this is the event the user dispatches to logout. it:
 ;; (a) calls the auth0 logout function, and
 ;; (b) dispatches an event to remove the user from the app db (and localstore)
 (fn [{:keys [db]} _]
   (let [client (get db :auth0-client)]
     (js/Promise. (fn [resolve reject]
                    (-> client
                        (.logout)
                        (.then (fn [_]
                                 (rf/dispatch [:auth/remove-auth0-user-from-db])
                                 (resolve true)))
                        (.catch (fn [err]
                                  (.log js/console "Error during logout: " err)
                                  (reject err)))))))))

(rf/reg-event-fx
 :auth/store-auth0-tokens-in-db
 (fn [{:keys [db]} [_ tokens]] ;; tokens here is the auth0 tokens object
   (.log js/console "Storing tokens in db: " tokens)
   (let [clean-tokens (js->clj tokens :keywordize-keys true)]
     {:db (assoc-in db [:auth :tokens] tokens)
      :dispatch [:auth/set-auth-in-ls]})))

(rf/reg-event-fx
 :auth/get-auth0-tokens
 ;; this is the event the user dispatches to get the auth0 tokens. it:
 ;; (a) calls the auth0 getTokenSilently function, and
 ;; (b) dispatches an event to store the tokens in the app db
 (fn [{:keys [db]} _]
   (let [client (get db :auth0-client)
         options (clj->js {
                          ;;  :detailedResponse? false
                           :cacheMode? "off"
                          ;;  :responseType "token"
                           :audience "https://readallaboutit.com.au/api/v1/"
                           :scope "read:users write:records"})]
     (.log js/console "Getting tokens from Auth0 client: " client " with options: " options)
     (js/Promise. (fn [resolve reject]
                    (-> client
                        (.getTokenSilently options)
                        (.then (fn [tokens]
                                 (.log js/console "Got tokens: " tokens)
                                 (rf/dispatch [:auth/store-auth0-tokens-in-db tokens])
                                 (resolve tokens)))
                        (.catch (fn [err]
                                  (.log js/console "Error getting tokens: " err)
                                  (reject err)))))))))


;; get token claims & store in app db
;; (rf/reg-event-fx
;;  :auth/get-auth0-token-claims
;;  (fn [{:keys [db]} _]
;;    (let [client (get db :auth0-client)]
;;      (js/Promise. (fn [resolve reject]
;;                     (-> client
;;                         (.get)
;;                         )
;;                     ))
     
;;      )
;;    )
;;  )










(rf/reg-event-db
 ;; print the auth0-client to the console
 :auth/print-auth0-client
 (fn [db _]
   (.log js/console "Auth0 client:" (get db :auth0-client))
   db))




;; ----------------------------------------------------------------------------
;; ------------------------ EXTRA AUTH STUFF ----------------------------------
;; ----------------------------------------------------------------------------
(defn auth-header
  "Get user token and format for API authorization"
  [db]
  (when-let [token (get-in db [:auth :tokens])]
    [:Authorization (str "Bearer " token)]))



(rf/reg-event-fx
 ;; translate a user email to our internal user id (if it exists) and store it
 ;; in the app db. this is used to link the auth0 user to our internal user id.
 ;; endpoint: GET @ /api/v1/user?email=<email> (ie, email as query param)
 :auth/get-user-id-from-db
 (fn [{:keys [db]} [_]]
   (let [email (-> db (get-in [:auth :user :email]))]
     {:http-xhrio {:method :get
                   :uri (endpoint "user")
                   :params {:email email}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:auth/store-user-id]
                   :on-failure [:auth/store-user-id-error-in-db]}})))

(rf/reg-event-fx
 :auth/store-user-id
 (fn [{:keys [db]} [_ response]]
   (let [user-id (-> response :id)]
     {:db (-> db (assoc-in [:auth :user-id] user-id))
      :dispatch [:auth/set-auth-in-ls]})))

(rf/reg-event-db
 :auth/store-user-id-error-in-db
 (fn [db [_ error]]
   (assoc-in db [:auth :user-id-error] error)))


;; TEMPORARY AUTH TEST @ /api/v1/authtest
;; TODO: REMOVE THIS
(rf/reg-event-fx
 :auth/test-auth
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri (endpoint "test")
                 :headers (auth-header db)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/store-auth-test]
                 :on-failure [:auth/store-auth-test-error-in-db]}}))

(rf/reg-event-fx
 :auth/test-auth-without-auth
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri (endpoint "test")
                ;;  :headers (auth-header db)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/store-auth-test]
                 :on-failure [:auth/store-auth-test-error-in-db]}}))

(rf/reg-event-db
 :auth/store-auth-test
 (fn [db [_ response]]
   (.log js/console "Auth test response:" response)
   (-> db
       (assoc-in [:auth-test] response)
       (dissoc :auth-test-error))))

(rf/reg-event-db
  :auth/store-auth-test-error-in-db
  (fn [db [_ error]]
    (assoc-in db [:auth-test-error] error)))



;; ----------------------------------------------------------------------------
;; ------------------------ NAVIGATION DISPATCHERS ----------------------------
;; ----------------------------------------------------------------------------

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


;; ----------------------------------------------------------------------------
;; ------------------------- DOCS PAGE DISPATCHERS ----------------------------
;; ----------------------------------------------------------------------------
;; --- These are used for fetching static content for the landing page etc. ---
;; ----------------------------------------------------------------------------

(rf/reg-event-fx
 :platform/fetch-about-txt
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/abouttxt"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:platform/set-about-page]}}))

(rf/reg-event-db
 :platform/set-about-page
 (fn [db [_ doc]]
   (assoc-in db [:static-content :about] doc)))

(rf/reg-event-fx
 :platform/fetch-faq-txt
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/faqtxt"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:platform/set-faq-page]}}))

(rf/reg-event-db
 :platform/set-faq-page
 (fn [db [_ doc]]
   (assoc-in db [:static-content :faq] doc)))

(rf/reg-event-fx
 :platform/fetch-team-txt
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/teamtxt"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:platform/set-team-page]}}))

(rf/reg-event-db
 :platform/set-team-page
 (fn [db [_ doc]]
   (assoc-in db [:static-content :team] doc)))

;; ----------------------------------------------------------------------------
;; ------------------------- INITIALIZE HOME PAGE -----------------------------
;; ----------------------------------------------------------------------------
;; --- This is used to initialize the home page when the app is loaded. -------
;; ----------------------------------------------------------------------------

(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-landing-page]}))


;; ----------------------------------------------------------------------------
;; ------------------------- MODAL MANAGEMENT ---------------------------------
;; ----------------------------------------------------------------------------

(rf/reg-event-db
 :app/show-modal
 (fn [db [_ modal-id]]
   (assoc-in db [:app/active-modals modal-id] true)))

(rf/reg-event-db
 :app/hide-modal
 (fn [db [_ modal-id]]
   (update db :app/active-modals dissoc modal-id)))

;; ----------------------------------------------------------------------------
;; ------------------------- GETTING RECORDS ----------------------------------
;; ----------------------------------------------------------------------------
;; --- These are used for fetching metadata & content of records for: ---------
;; --- 1. newspapers (eg '/newspaper/1224') -----------------------------------
;; --- 2. authors (eg '/author/1224') -----------------------------------------
;; --- 3. titles (ie, stories) (eg '/title/1224') -----------------------------
;; --- 4. chapters (eg '/chapter/1224') ---------------------------------------
;; ----------------------------------------------------------------------------

;; --- GET Newspaper (Metadata) @ /newspaper/:id ------------------------------
(rf/reg-event-fx
 :newspaper/get-newspaper
 (fn [{:keys [db]} [_ pos-id]]
   (let [id (-> db :common/route :path-params :id)]
     {:db (assoc db :newspaper/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "newspaper" (if id id pos-id))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:newspaper/newspaper-loaded]
                   :on-failure      [:newspaper/newspaper-load-failed]}})))

(rf/reg-event-fx
 :newspaper/newspaper-loaded
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :newspaper/metadata-loading? false)
            (assoc :newspaper/details response)
            (assoc :newspaper/error nil)
            (update-in [:tbc/records :newspapers] conj response)
            (update-in [:tbc/records :newspapers] distinct))
    :dispatch-n (cond (str/includes? (-> db :common/route :path) "/edit/newspaper")
                      [[:newspaper/populate-edit-newspaper-form]] ;; TODO: this is a hack
                      :else [])}))

(rf/reg-event-db
 :newspaper/newspaper-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/metadata-loading? false)
       (assoc :newspaper/error (:message response)))))


;; --- GET Titles in Newspaper @ /newspaper/:id/titles ------------------------
(rf/reg-event-fx
 :newspaper/get-titles-in-newspaper
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

;; --- GET Title (Metadata) @ /title/:id --------------------------------------

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

(rf/reg-event-fx
 :title/title-loaded
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :title/metadata-loading? false)
            (assoc :title/details response)
            (assoc :title/error nil)
            (update-in [:tbc/records :titles] conj response)
            (update-in [:tbc/records :titles] distinct))
    :dispatch-n (cond (str/includes? (-> db :common/route :path) "/edit/title")
                      [[:title/populate-edit-title-form]] ;; TODO: this is a hack
                      ;; (str/includes? (-> db :common/route :path) "/title")
                      ;; [[:title/get-chapters-in-title]]
                      :else [])}))

(rf/reg-event-db
 :title/title-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :title/metadata-loading? false)
       (assoc :title/error (or (:message response)
                               (get-in response [:response :responseText])
                               "Unknown error")))))

;; --- GET Chapters in Title @ /title/:id/chapters ----------------------------
(rf/reg-event-fx
 :title/get-chapters-in-title
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

;; --- GET Chapter @ /chapter/:id ---------------------------------------------

(rf/reg-event-fx
 :chapter/get-chapter
 (fn [{:keys [db]} [_ pos-id]]
   (let [path-id (-> db :common/route :path-params :id)]
     {:db (assoc db :chapter/loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "chapter" (if path-id path-id pos-id))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:chapter/chapter-loaded]
                   :on-failure      [:chapter/chapter-load-failed]}})))

(rf/reg-event-fx
 :chapter/chapter-loaded
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :chapter/loading? false)
            (assoc :chapter/details response)
            (assoc :chapter/error nil)
            (update-in [:tbc/records :chapters] conj response) ;; add the chapter record
            (update-in [:tbc/records :chapters] distinct)) ;; remove duplicates
    :dispatch-n (cond (str/includes? (-> db :common/route :path) "/edit/chapter")
                      [[:chapter/populate-edit-chapter-form]] ;; TODO: this is a hack
                      :else [])}))

(rf/reg-event-db
 :chapter/chapter-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/loading? false)
       (assoc :chapter/error (or (:message response)
                                 (get-in response [:response :message])
                                 "Unknown error")))))

;; --- GET Author (Metadata) @ /author/:id ------------------------------------
(rf/reg-event-fx
 :author/get-author
 (fn [{:keys [db]} [_ pos-id]]
   (let [path-id (-> db :common/route :path-params :id)]
     {:db (assoc db :author/metadata-loading? true)
      :http-xhrio {:method          :get
                   :uri             (endpoint "author" (if path-id path-id pos-id))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:author/author-loaded]
                   :on-failure      [:author/author-load-failed]}})))

(rf/reg-event-fx
 :author/author-loaded
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :author/metadata-loading? false)
            (assoc :author/details response)
            (assoc :author/error nil)
            (update-in [:tbc/records :authors] conj response)
            (update-in [:tbc/records :authors] distinct))
    :dispatch-n (cond (str/includes? (-> db :common/route :path) "/edit/author")
                      [[:author/populate-edit-author-form]] ;; TODO: this is a hack
                      :else [])}))


(rf/reg-event-db
 :author/author-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :author/metadata-loading? false)
       (assoc :author/error (:message response)))))


;; --- GET Titles by Author @ /author/:id/titles ------------------------------
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








;; ----------------------------------------------------------------------------
;; ------------------------- SEARCH -------------------------------------------
;; ----------------------------------------------------------------------------
;; --- These are used for searching for records of various types. -------------
;; ----------------------------------------------------------------------------



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
 (fn [{:keys [db]} [_ ]]
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

(rf/reg-event-fx
 :search/process-search-results
 (fn [{:keys [db]} [_ response]]
   (let [is-first-page? (or (= 0 (:offset response)) (nil? (:offset response)))
         new-results (if is-first-page? (:results response) (concat (:search/results db) (:results response)))
         needs-more? (= (:limit response) (count (:results response)))  ;; todo: this is a hack; need to fix this in the API
         new-query-params (if needs-more?
                            (-> db :common/route :query-params
                                (assoc :offset (+ (:offset response) (:limit response))))
                            (-> db :common/route :query-params))]
     {:db (-> db
              (assoc :search/loading? false)
              (assoc :search/results new-results)
              (assoc :search/type (:search_type response))
              (assoc :search/time-loaded (js/Date.now))
              (assoc-in [:common/route :query-params] new-query-params))
      :dispatch-n (if needs-more?
                    [[:search/submit-titles-search]]
                    [])})))

(rf/reg-event-db
 :search/process-search-error
 (fn [db [_ response]]
   (-> db
       (assoc :search/loading? false)
       (assoc :search/error (:message response)))))



(rf/reg-event-db
 :newspaper/clear-newspaper
 ;; remove :newspaper/metadata-loading? :newspaper/error and :newspaper/details from db
 (fn [db _]
   (-> db
       (dissoc :newspaper/metadata-loading?)
       (dissoc :newspaper/titles-loading?)
       (dissoc :newspaper/error)
       (dissoc :newspaper/details)
       (dissoc :newspaper/titles))))



;; VIEWING AN AUTHOR


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










;; VIEWING A TITLE



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











;; VIEWING A CHAPTER



(rf/reg-event-db
 :chapter/clear-chapter
 ;; remove :chapter/loading? :chapter/error and :chapter/details from db (but keep in :tbc/records)
 (fn [db _]
   (-> db
       (dissoc :chapter/loading?)
       (dissoc :chapter/error)
       (dissoc :chapter/details))))








;; ADDING NEW NEWSPAPER/STORY/CHAPTER RECORDS (FRONTEND ONLY)

;; NEW NEWSPAPER
(rf/reg-event-db
 :newspaper/update-new-newspaper-form-field
 (fn [db [_ field value]]
   (assoc-in db [:newspaper/new-newspaper-form field] value)))

;; NEW AUTHOR
(rf/reg-event-db
 :author/update-new-author-form-field
 (fn [db [_ field value]]
   (assoc-in db [:author/new-author-form field] value)))

;; NEW TITLE
(rf/reg-event-db
 :title/update-new-title-form-field
 (fn [db [_ field value]]
   (assoc-in db [:title/new-title-form field] value)))


;; NEW CHAPTER
(rf/reg-event-db ;; update a field in the new chapter form
 :chapter/update-new-chapter-form-field ;; usage: (dispatch [:chapter/update-new-chapter-form-field :field-name "new value"])
 (fn [db [_ field value]]
   (assoc-in db [:chapter/new-chapter-form field] value)))

(rf/reg-event-db
 :chapter/populate-new-chapter-form ;; populate the new chapter form with data from a trove article result (already in the db at :trove/details)
 (fn [db [_]]
   (let [trove-details (-> db
                           (get-in [:trove/details] {})
                           (dissoc :trove_newspaper_url :trove_article_id))]
     (update-in db [:chapter/new-chapter-form] merge trove-details))))

(rf/reg-event-fx
 :title/prepop-new-title-form-from-query-params ;; dispatched when navigating to /add/title?author_id=123 to prepopulate :title/new-title-form with author_id field etc
 (fn [{:keys [db]} [_]]
   (let [query-params (-> db (get-in [:common/route :query-params] {}))]
     {:db (-> db
              (update-in [:title/new-title-form] merge query-params)
              ;; ensure that author_id and newspaper_table_id are integers
              ;; (update-in [:title/new-title-form :author_id] #(if (string? %) (js/parseInt %) %))
              ;; (update-in [:title/new-title-form :newspaper_table_id] #(if (string? %) (js/parseInt %) %))
              )
      :dispatch-n [(when (:author_id query-params)
                     [:author/get-author (:author_id query-params)]) ;; dispatch event to get author details if prepopulating from query params
                   (when (:newspaper_table_id query-params)
                     [:newspaper/get-newspaper (:newspaper_table_id query-params)]) ;; dispatch event to get newspaper details if prepopulating from query params
                   [:platform/get-newspaper-options]
                   [:platform/get-author-options]]})))



(rf/reg-event-db
 :chapter/clear-new-chapter-form
 (fn [db [_]]
   (-> db
       (dissoc :chapter/new-chapter-form
               :chapter/creation-error
               :chapter/creation-success
               :chapter/creation-submission
               :chapter/creating?
               :title/details
               :title/error
               :title/metadata-loading?
               :title/chapters-loading?
               :title/chapters
               :trove/details
               :trove/error
               :trove/loading?))))

(rf/reg-event-db
 :title/clear-new-title-form
 (fn [db [_]]
   (-> db
       (dissoc :title/new-title-form
               :title/creation-error
               :title/creation-success
               :title/creation-submission
               :title/creating?))))

(rf/reg-event-db
 :author/clear-new-author-form
 (fn [db [_]]
   (-> db
       (dissoc :author/new-author-form
               :author/creation-error
               :author/creation-success
               :author/creation-submission
               :author/creating?))))



(rf/reg-event-fx
 :chapter/prepop-new-chapter-form-from-query-params ;; dispatched when navigating to /add/chapter?title_id=123 to prepopulate :chapter/new-chapter-form with title_id field etc
 (fn [{:keys [db]} [_]]
   (let [query-params (-> db (get-in [:common/route :query-params] {}))]
     {:db (update-in db [:chapter/new-chapter-form] merge query-params)
      :dispatch-n [(when (:title_id query-params)
                     [:title/get-title (:title_id query-params)]) ;; dispatch event to get title details if prepopulating from query params
                   (when (:trove_article_id query-params)
                     [:trove/get-chapter (:trove_article_id query-params)]) ;; dispatch event to get trove details if prepopulating from query params
                   ]})))




(rf/reg-event-db
 :title/update-edit-title-form-field
 (fn [db [_ field value]]
   (assoc-in db [:title/edit-title-form field] value)))


(rf/reg-event-db
 :title/populate-edit-title-form ;; populate the edit-title-form with the title details
 (fn [db [_]]
   (let [title-details (-> db
                           (get-in [:title/details])
                           (select-keys [:id :author_id :newspaper_table_id

                                         :span_start :span_end :publication_title :common_title :length

                                         :attributed_author_name
                                         :author_of :inscribed_author_nationality
                                         :inscribed_author_gender
                                         :also_published :name_category

                                         :information_source :additional_info]))]
     (update-in db [:title/edit-title-form] merge title-details))))


(rf/reg-event-db
 :title/clear-edit-title-form
 (fn [db [_]]
   (-> db
       (dissoc :title/edit-title-form
               :title/update-error
               :title/update-success
               :title/update-submission
               :title/updating?
               :title/details
               :title/error
               :title/metadata-loading?))))

(rf/reg-event-db
 :author/update-edit-author-form-field
 (fn [db [_ field value]]
   (assoc-in db [:author/edit-author-form field] value)))


(rf/reg-event-db
 :author/populate-edit-author-form ;; populate the edit-author-form with the author details
 (fn [db [_]]
   (let [author-details (-> db
                            (get-in [:author/details])
                            (select-keys [:id :common_name :other_name :gender
                                          :nationality :nationality_details
                                          :author_details]))]
     (update-in db [:author/edit-author-form] merge author-details))))


(rf/reg-event-db
 :author/clear-edit-author-form
 (fn [db [_]]
   (-> db
       (dissoc :author/edit-author-form
               :author/update-error
               :author/update-success
               :author/update-submission
               :author/updating?
               :author/details
               :author/error
               :author/metadata-loading?))))


(rf/reg-event-db
 :newspaper/update-edit-newspaper-form-field
 (fn [db [_ field value]]
   (assoc-in db [:newspaper/edit-newspaper-form field] value)))


(rf/reg-event-db
 :newspaper/populate-edit-newspaper-form ;; populate the edit-newspaper-form with the newspaper details
 (fn [db [_]]
   (let [newspaper-details (-> db (get-in [:newspaper/details]))]
     (update-in db [:newspaper/edit-newspaper-form] merge newspaper-details))))


(rf/reg-event-db
 :newspaper/clear-edit-newspaper-form
 (fn [db [_]]
   (-> db
       (dissoc :newspaper/edit-newspaper-form
               :newspaper/update-error
               :newspaper/update-success
               :newspaper/update-submission
               :newspaper/updating?
               :newspaper/details
               :newspaper/error
               :newspaper/metadata-loading?))))


(rf/reg-event-db
 :chapter/populate-edit-chapter-form ;; populate the edit-chapter-form with the chapter details
 (fn [db [_]]
   (let [chapter-details (-> db
                             (get-in [:chapter/details])
                             (select-keys [:chapter_title
                                           :chapter_number
                                           :final_date
                                           :id
                                           :title_id
                                           :trove_article_id]))]
     (update-in db [:chapter/edit-chapter-form] merge chapter-details))))

(rf/reg-event-db
 :chapter/update-edit-chapter-form-field
 (fn [db [_ field value]]
   (assoc-in db [:chapter/edit-chapter-form field] value)))

(rf/reg-event-db
 :chapter/clear-edit-chapter-form
 (fn [db [_]]
   (-> db
       (dissoc :chapter/edit-chapter-form
               :chapter/update-error
               :chapter/update-success
               :chapter/update-submission
               :chapter/updating?
               :chapter/details
               :chapter/error
               :chapter/metadata-loading?))))









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

(rf/reg-event-fx
 ;; event for dispatching the http request to the api to 'get newspaper options' about the platform
 ;; (this is a terse list of all newspapers in the database, used for the 'newspaper' field in the 'add title' form)
 :platform/get-newspaper-options
 (fn [{:keys [db]} [_]]
   {:db (assoc-in db [:platform/newspaper-options :loading?] true)
    :http-xhrio {:method          :get
                 :uri             (endpoint "options" "newspapers")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:platform/newspaper-options-loaded]
                 :on-failure      [:platform/newspaper-options-load-failed]}}))

(rf/reg-event-db
 :platform/newspaper-options-loaded
 (fn [db [_ response]]
   (-> db
       (assoc-in [:platform/newspaper-options :loading?] false)
       (assoc-in [:tbc/terse-records :newspapers] response))))

(rf/reg-event-db
 :platform/newspaper-options-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc-in [:platform/newspaper-options :loading?] false)
       (assoc-in [:platform/newspaper-options :error] (:message response)))))

(rf/reg-event-fx
 ;; event for dispatching the http request to the api to 'get author options' about the platform
 ;; (this is a terse list of all authors in the database, used for the 'author' field in the 'add title' form)
 :platform/get-author-options
 (fn [{:keys [db]} [_]]
   {:db (assoc-in db [:platform/author-options :loading?] true)
    :http-xhrio {:method          :get
                 :uri             (endpoint "options" "authors")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:platform/author-options-loaded]
                 :on-failure      [:platform/author-options-load-failed]}}))

(rf/reg-event-db
 :platform/author-options-loaded
 (fn [db [_ response]]
   (-> db
       (assoc-in [:platform/author-options :loading?] false)
       (assoc-in [:tbc/terse-records :authors] response))))

(rf/reg-event-db
 :platform/author-options-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc-in [:platform/author-options :loading?] false)
       (assoc-in [:platform/author-options :error] (:message response)))))



























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

(rf/reg-event-fx
 :trove/chapter-loaded ;; append the chapter to the db at [:trove/records :chapters], and replace whatever is in :trove/details
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :trove/loading? false)
            (assoc :trove/error nil)
            (assoc :trove/details response)
            (update-in [:trove/records :chapters] conj response)
            (update-in [:trove/records :chapters] distinct))
    :dispatch-n [[:trove/get-chapter-exists (:trove_article_id response)]] ;; check if the chapter exists in the db
    }))




(rf/reg-event-db
 :trove/chapter-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :trove/loading? false)
       (assoc :trove/error (:message (:response response))))))


;; --- PUT Trove Chapter @ /api/v1/trove/chapter/:trove_article_id ------------
;; --- (updates the chapter in our db with content from Trove) ----------------
(rf/reg-event-fx
 :trove/put-chapter
 (fn [{:keys [db]} [_ trove-article-id]] 
   {:db (assoc db :trove/loading? true)
    :http-xhrio {:method          :put
                 :uri             (endpoint "trove" "chapter" trove-article-id)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:trove/chapter-put-success]
                 :on-failure      [:trove/chapter-put-failed]}}))

(rf/reg-event-fx
 :trove/chapter-put-success
 (fn [{:keys [db]} [_ response]]
   (.log js/console "chapter-put-success" response)
   {:db (-> db
            (assoc :trove/loading? false)
            (assoc :trove/error nil)
            (assoc :trove/details response)
            (update-in [:trove/records :chapters] conj response)
            (update-in [:trove/records :chapters] distinct))
    :dispatch [:chapter/get-chapter (:id response)]}))

(rf/reg-event-db
 :trove/chapter-put-failed
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
       (update-in [:trove/ids-already-in-db :chapters]
                  conj (if (:exists response) (:trove_article_id response) nil))
       (update-in [:trove/ids-already-in-db :chapters] distinct))))


(rf/reg-event-db
 :trove/chapter-exists-load-failed
 (fn [db [_ response]]
   (-> db
       (assoc :trove/loading? false)
       (assoc :trove/error (:message response)))))






;; EVENT HANDLERS FOR CREATING A NEW CHAPTER/TITLE/NEWSPAPER/AUTHOR


;; --- POST Chapter @ /api/v1/create/chapter ----------------------------------

(rf/reg-event-fx
 :chapter/create-new-chapter
 (fn [{:keys [db]} [_ chapter]]
   (let [user-id (-> db :auth :user-id)]
     {:db (-> db
              (assoc :chapter/creating? true)
              (assoc :chapter/creation-submission chapter))
      :http-xhrio {:method          :post
                   :uri             (endpoint "create" "chapter")
                   :params             (-> chapter
                                           (update-in [:trove_article_id] ;; ensure that it's an integer
                                                      #(if (string? %) (js/parseInt %) %))
                                           (update-in [:title_id]
                                                      #(if (string? %) (js/parseInt %) %))
                                           (assoc :added_by user-id)
                                           (update-in [:added_by]
                                                      #(if (string? %) (js/parseInt %) %)))
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:chapter/new-chapter-created]
                   :on-failure      [:chapter/new-chapter-create-failed]}})))


(rf/reg-event-db
 :chapter/new-chapter-created
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/creating? false)
       (assoc :chapter/creation-success response))))

(rf/reg-event-db
 :chapter/new-chapter-create-failed
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/creating? false)
       (assoc :chapter/creation-error response))))


;; --- POST Title @ /api/v1/create/title --------------------------------------
(rf/reg-event-fx
 :title/create-new-title
 (fn [{:keys [db]} [_ title]]
   (let [user-id (-> db :auth :user-id)]
     {:db (-> db
              (assoc :title/creating? true)
              (assoc :title/creation-submission title))
      :http-xhrio {:method          :post
                   :uri             (endpoint "create" "title")
                   :params             (-> title
                                           (update-in [:author_id] ;; ensure that it's an integer
                                                      #(if (string? %) (js/parseInt %) %))
                                           (update-in [:title_id]
                                                      #(if (string? %) (js/parseInt %) %))
                                           (update-in [:length]
                                                      #(if (string? %) (js/parseInt %) %))
                                           (assoc :added_by user-id)
                                           (update-in [:added_by]
                                                      #(if (string? %) (js/parseInt %) %)))
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:title/new-title-created]
                   :on-failure      [:title/new-title-create-failed]}})))


(rf/reg-event-db
 :title/new-title-created
 (fn [db [_ response]]
   (-> db
       (assoc :title/creating? false)
       (assoc :title/creation-success response))))

(rf/reg-event-db
 :title/new-title-create-failed
 (fn [db [_ response]]
   (-> db
       (assoc :title/creating? false)
       (assoc :title/creation-error response))))


;; --- POST Newspaper @ /api/v1/create/newspaper ------------------------------
;; TODO: THIS


;; --- POST Author @ /api/v1/create/author ------------------------------------
(rf/reg-event-fx
 :author/create-new-author
 (fn [{:keys [db]} [_ author]]
   (let [user-id (-> db :auth :user-id)]
     {:db (-> db
              (assoc :author/creating? true)
              (assoc :author/creation-submission author))
      :http-xhrio {:method          :post
                   :uri             (endpoint "create" "author")
                   :params             (-> author
                                           (update-in [:trove_article_id] ;; ensure that it's an integer
                                                      #(if (string? %) (js/parseInt %) %))
                                           (update-in [:title_id]
                                                      #(if (string? %) (js/parseInt %) %))
                                           (assoc :added_by user-id)
                                           (update-in [:added_by]
                                                      #(if (string? %) (js/parseInt %) %)))
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:author/new-author-created]
                   :on-failure      [:author/new-author-create-failed]}})))


(rf/reg-event-db
 :author/new-author-created
 (fn [db [_ response]]
   (-> db
       (assoc :author/creating? false)
       (assoc :author/creation-success response))))

(rf/reg-event-db
 :author/new-author-create-failed
 (fn [db [_ response]]
   (-> db
       (assoc :author/creating? false)
       (assoc :author/creation-error response))))











;; EVENT HANDLERS FOR UPDATING EXISTING CHAPTER/TITLE/NEWSPAPER/AUTHOR RECORDS

;; --- PUT Chapter @ /api/v1/chapter/:chapter_id ------------------------------

(rf/reg-event-fx
 :chapter/update-chapter
 (fn [{:keys [db]} [_ chapter]]
   {:db (-> db
            (assoc :chapter/updating? true)
            (assoc :chapter/update-submission chapter))
    :http-xhrio {:method          :put
                 :uri             (endpoint "chapter" (:id chapter))
                 :params          (-> chapter
                                      (dissoc :id)
                                      (update-in [:title_id]
                                                 #(if (string? %) (js/parseInt %) %)))
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:chapter/chapter-updated]
                 :on-failure      [:chapter/chapter-update-failed]}}))

(rf/reg-event-fx
 :chapter/chapter-updated
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :chapter/updating? false)
            (assoc :chapter/update-success response))
    :dispatch-n [[:chapter/get-chapter (:id response)]]}))

(rf/reg-event-db
 :chapter/chapter-update-failed
 (fn [db [_ response]]
   (-> db
       (assoc :chapter/updating? false)
       (assoc :chapter/update-error response))))


;; --- PUT Title @ /api/v1/title/:title_id ------------------------------------

(rf/reg-event-fx
 :title/update-title
 (fn [{:keys [db]} [_ title]]
   {:db (-> db
            (assoc :title/updating? true)
            (assoc :title/update-submission title))
    :http-xhrio {:method          :put
                 :uri             (endpoint "title" (:id title))
                 :params          (-> title
                                      (dissoc :id)
                                      (update-in [:length]
                                                 #(if (string? %) (js/parseInt %) %))
                                      (update-in [:newspaper_table_id]
                                                 #(if (string? %) (js/parseInt %) %)))
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:title/title-updated]
                 :on-failure      [:title/title-update-failed]}}))

(rf/reg-event-fx
 :title/title-updated
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :title/updating? false)
            (assoc :title/update-success response))
    :dispatch-n [[:title/get-title (:id response)]]}))

(rf/reg-event-db
 :title/title-update-failed
 (fn [db [_ response]]
   (-> db
       (assoc :title/updating? false)
       (assoc :title/update-error response))))

;; --- PUT Newspaper @ /api/v1/newspaper/:newspaper_id ------------------------

(rf/reg-event-fx
 :newspaper/update-newspaper
 (fn [{:keys [db]} [_ newspaper]]
   {:db (-> db
            (assoc :newspaper/updating? true)
            (assoc :newspaper/update-submission newspaper))
    :http-xhrio {:method          :put
                 :uri             (endpoint "newspaper" (:id newspaper))
                 :params          (-> newspaper
                                      (dissoc :id)
                                      (update-in [:trove_newspaper_id]
                                                 #(if (string? %) (js/parseInt %) %)))
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:newspaper/newspaper-updated]
                 :on-failure      [:newspaper/newspaper-update-failed]}}))

(rf/reg-event-fx
 :newspaper/newspaper-updated
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :newspaper/updating? false)
            (assoc :newspaper/update-success response))
    :dispatch-n [[:newspaper/get-newspaper (:id response)]]}))

(rf/reg-event-db
 :newspaper/newspaper-update-failed
 (fn [db [_ response]]
   (-> db
       (assoc :newspaper/updating? false)
       (assoc :newspaper/update-error response))))


;; --- PUT Author @ /api/v1/author/:author_id ---------------------------------

(rf/reg-event-fx
 :author/update-author
 (fn [{:keys [db]} [_ author]]
   {:db (-> db
            (assoc :author/updating? true)
            (assoc :author/update-submission author))
    :http-xhrio {:method          :put
                 :uri             (endpoint "author" (:id author))
                 :params          (-> author (dissoc :id))
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:author/author-updated]
                 :on-failure      [:author/author-update-failed]}}))

(rf/reg-event-fx
 :author/author-updated
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc :author/updating? false)
            (assoc :author/update-success response))
    :dispatch-n [[:author/get-author (:id response)]]}))

(rf/reg-event-db
 :author/author-update-failed
 (fn [db [_ response]]
   (-> db
       (assoc :author/updating? false)
       (assoc :author/update-error response))))












;; --- WEIRD EXTRA 'UPDATE' EVENTS DISPATCHERS ---------------------------------

;; --- 'Update Chapter From Trove' ---------------------------------------------
