(ns cde.components.modals
  "Reusable modal dialog components.

  Provides Bulma-styled modal cards with show/hide state managed
  via re-frame. Used for lookup pickers and confirmation dialogs."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [cde.events]
   [cde.subs]))

(defn modal-card
  "Renders a Bulma modal card with header, body, and footer sections.

  Visibility is controlled by the `:app/modal-showing?` subscription.
  Clicking the background or close button dispatches `:app/hide-modal`.

  Arguments:
  - `id` - unique modal identifier for state management
  - `title` - modal header title
  - `body` - main content component
  - `footer` - footer content (typically action buttons)"
  [id title body footer]
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

(defn modal-button
  "Renders a button that opens a modal card when clicked.

  Combines a trigger button with the modal-card component.

  Arguments:
  - `id` - unique modal identifier
  - `title` - button text and modal header title
  - `body` - modal body content
  - `footer` - modal footer content
  - `class` - optional CSS class for the button (default: is-primary)"
  ([id title body footer]
   [:div
    [:button.button.is-primary
     {:on-click #(rf/dispatch [:app/show-modal id])}
     title]
    [modal-card id title body footer]])
  ([id title body footer class]
   [:div
    [:button.button
     {:class class
      :on-click #(rf/dispatch [:app/show-modal id])}
     title]
    [modal-card id title body footer]]))
