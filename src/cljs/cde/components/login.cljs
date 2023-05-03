(ns cde.components.login
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ajax.core :refer [POST]]
   [clojure.string :as string]
   [cde.events]
   [cde.subs]
   [cde.components.modals :refer [modal-button]]))

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
         {:type "email"
          :placeholder "Your email..."
          :value (:email @fields)
          :on-change #(swap! fields assoc :email (.. % -target -value))}]]]
      [:div-field
       [:div.label "Password"]
       [:div.control
        [:input.input
         {:type "password"
          :placeholder "Your password..."
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
                                 (rf/dispatch [:app/hide-modal :user/register])
                                 (rf/dispatch [:app/show-modal :user/login]))
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
          :placeholder "Your username..."
          :value (:username @fields)
          :on-change #(swap! fields assoc :username (.. % -target -value))}]]]
      [:div-field
       [:div.label "Email"]
       [:div.control
        [:input.input
         {:type "email"
          :placeholder "Your email..."
          :value (:email @fields)
          :on-change #(swap! fields assoc :email (.. % -target -value))}]]]
      [:div-field
       [:div.label "Password"]
       [:div.control
        [:input.input
         {:type "password"
          :placeholder "Your password..."
          :value (:password @fields)
          :on-change #(swap! fields assoc :password (.. % -target -value))}]]]
      ; show warning if password is too short
      (when (and (not (string/blank? (:password @fields))) (< (count (:password @fields)) 8))
        [:div.notification.is-warning
         "Password must be at least 8 characters long."])
      ; show confirm password field if password is long enough
      (when (>= (count (:password @fields)) 8)
        [:div-field
         [:div.label "Confirm Password"]
         [:div.control
          [:input.input
           {:type "password"
            :placeholder "Confirm your password..."
            :value (:confirm @fields)
            :on-change #(swap! fields assoc :confirm (.. % -target -value))}]]])
      ; show warning if password is long enough but doesn't match confirm password
      (when (and (>= (count (:password @fields)) 8)
                 (not= (:password @fields) (:confirm @fields)))
        [:div.notification.is-warning
         "Passwords do not match."])]
     ;; Footer
     [:button.button.is-primary.is-fullwidth
      {:on-click do-register
       :disabled (or (string/blank? (:username @fields))
                     (string/blank? (:email @fields))
                     (string/blank? (:password @fields))
                     (string/blank? (:confirm @fields)))}
      "Create Account"]]))


(defn logout-button []
  [:button.button
   {:on-click #(POST ; TODO: FIX THIS POST CALL!
                 "/api/logout"
                 {:handler (fn [_]
                             (rf/dispatch [:auth/handle-logout]))})}
   "Log Out"])



(defn nameplate [{:keys [email]}]
  ;; a 'nameplate' button that navigates to the user's profile
  [:button.button.is-primary
   email])