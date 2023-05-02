(ns cde.components.modals
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ajax.core :refer [GET POST]] 
   [clojure.string :as string]
   [cde.events]))


(defn modal-card [id title body footer]
  [:div.modal
   {:class (when @(rf/subscribe [:app/modal-showing? id]) "is-active")}
   [:div.modal-background
    {:on-click #(rf/dispatch [:app/hide-modal id])}]
   [:div.modal-card
    [:header.modal-card-head
     [:p.modal-card-title title]
     [:button.delete
      {:on-click #(rf/dispatch [:app/hide-modal id])}]]
    [:section.modal-card-body body]
    [:footer.modal-card-foot footer]]])

(defn modal-button [id title body footer]
  [:div
   [:button.button.is-primary
    {:on-click #(rf/dispatch [:app/show-modal id])}
    title]
   [modal-card id title body footer]])

(defn login-button []
  (r/with-let
    [fields (r/atom {})
     error (r/atom nil)
     do-login (fn [_]
                (reset! error nil)
                (POST
                  "/api/login"
                  {:headers {"Accept" "application/transit+json"}
                   :params @fields
                   :handler (fn [response]
                              (reset! fields {})
                              (rf/dispatch [:auth/handle-login response])
                              (rf/dispatch [:app/hide-modal :user/login]))
                   :error-handler (fn [error-response]
                                    (reset! error
                                            (or (:message (:response error-response))
                                                (:status-text error-response)
                                                "Unknown Error")))}))]
    [modal-button :user/login
     ;; Title
     "Login"
     ;; Body
     [:div
      (when-not (string/blank? @error)
        [:div.notification.is-danger
         @error])
      [:div-field
       [:div.label "Email"]
       [:div.control
        [:input.input
         {:type "text"
          :value (:email @fields)
          :on-change #(swap! fields assoc :email (.. % -target -value))}]]]
      [:div-field
       [:div.label "Password"]
       [:div.control
        [:input.input
         {:type "password"
          :value (:password @fields)
          :on-change #(swap! fields assoc :password (.. % -target -value))
          :on-key-down #(when (= (.-keyCode %) 13) (do-login))}]]]]
     ;; Footer
     [:button.button.is-primary.is-fullwidth
      {:on-click do-login
       :disabled (or (string/blank? (:email @fields))
                     (string/blank? (:password @fields)))}
      "Log In"]]))



(defn register-button []
  (r/with-let
    [fields (r/atom {})
     error (r/atom nil)
     do-register (fn [_]
                (reset! error nil)
                (POST
                  "/api/register"
                  {:headers {"Accept" "application/transit+json"}
                   :params @fields
                   :handler (fn [response]
                              (reset! fields {})
                              (rf/dispatch [:app/hide-modal :user/register]))
                   :error-handler (fn [error-response]
                                    (reset! error
                                            (or (:message (:response error-response))
                                                (:status-text error-response)
                                                "Unknown Error")))}))]
    [modal-button :user/register
     ;; Title
     "Sign Up"
     ;; Body
     [:div
      (when-not (string/blank? @error)
        [:div.notification.is-danger
         @error])
      [:div-field
       [:div.label "Username"]
       [:div.control
        [:input.input
         {:type "text"
          :value (:username @fields)
          :on-change #(swap! fields assoc :username (.. % -target -value))}]]]
      [:div-field
       [:div.label "Email"]
       [:div.control
        [:input.input
         {:type "text"
          :value (:email @fields)
          :on-change #(swap! fields assoc :email (.. % -target -value))}]]]
      [:div-field
       [:div.label "Password"]
       [:div.control
        [:input.input
         {:type "password"
          :value (:password @fields)
          :on-change #(swap! fields assoc :password (.. % -target -value))}]]]
      [:div-field
       [:div.label "Confirm Password"]
       [:div.control
        [:input.input
         {:type "password"
          :value (:confirm @fields)
          :on-change #(swap! fields assoc :confirm (.. % -target -value))}]]]
      ]
     ;; Footer
     [:button.button.is-primary.is-fullwidth
      {:on-click do-register
       :disabled (or (string/blank? (:username @fields))
                     (string/blank? (:email @fields))
                     (string/blank? (:password @fields))
                     (string/blank? (:confirm @fields)))}
      "Register"]]))


(defn logout-button []
  [:button.button
   {:on-click #(POST ; TODO: FIX THIS POST CALL!
                 "/api/logout"
                 {:handler (fn [_]
                             (rf/dispatch [:auth/handle-logout]))})}
   "Log Out"])

(defn nameplate [{:keys [email]}]
  [:button.button.is-primary email])