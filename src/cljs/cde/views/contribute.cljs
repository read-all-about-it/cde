(ns cde.views.contribute
  "Contribution guide page for community members.

  Displays information about how users can contribute to the
  To Be Continued database by adding newspaper fiction records.
  Shows different content based on authentication status."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.components.nav :refer [page-header contribute-block]]))

(defn contribute-page
  "Renders the contribution guide page.

  For authenticated users, displays the contribute-block component with
  options to add records. For unauthenticated users, shows a login prompt."
  []
  (r/with-let [logged-in? (rf/subscribe [:auth/logged-in?])]
    (fn []
      [:section.section>div.container>div.content
       [page-header "Contribute to the Database"]
       [:div.block.has-text-centered
        [:p [:em "To Be Continued"] " is a community effort."]
        (if @logged-in?
          [contribute-block]
          [:div
           [:p "If you've found newspaper fiction, we'd love your contribution!"]
           [:a.button.button.is-primary {:on-click #(rf/dispatch [:auth/login-auth0-with-popup])} "Login to Contribute"]])]])))
