(ns cde.components.forms
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]
   [cde.components.modals :refer [modal-button]]
   [cde.utils :refer [key->help key->title key->placeholder]]))


(defn simple-ff-block
  "A simple container for a list of form fields."
  [& fields]
  [:div
   (for [field fields]
     field)])

(defn tabbed-form
  "A complex form component, with tabs (determining visible fields)
   and a submit button (visible on all tabs).

   Takes:
   - tabs - a vec of maps, each with
         :tab-title (a string)
         :content (a component, usually containing a block of form fields)
         :danger? (optional, default false); if true, the tab title will be red
   - visible-tab - an atom containing the index of the currently visible tab
   - footer - an always-visible component to display below the tab-specific fields"
  [& {:keys [tabs visible-tab footer]}]
  [:div
   [:div
    [:div.tabs.is-centered.is-boxed ;; tab navigation 'header'
     [:ul ;; list of tabs; on click, set visible-tab to index of tab
      (for [[idx tab-title] (map-indexed vector (map :tab-title tabs))]
        [:li {:class (if (= idx @visible-tab) "is-active" "")
              :on-click #(reset! visible-tab idx)}
         [:a [:span tab-title]]])]] ;; TODO: Implement danger-zone text styling for tabs
    [:div ;; tab-specific fields
     [:h3 (get-in tabs [@visible-tab :tab-title])]
     (get-in tabs [@visible-tab :content])]]
   ;; 'footer' (usually a submit button) is always visible
   [:div.section
    [:div.block.has-text-right
     footer]]])


(defn labelled-text-field
  "A labelled horizontal form field, with a text input.
   
   Takes map with keys:
   - label - the label to display
   - placeholder - the placeholder text to display in the input
   - value - the value source of the input
   - on-change - a function to call when the input changes
   - class - optional; a class to add to the input (can be an atom, a string, or an if/cond etc)
   - disabled - optional; when to disable the input (can be an atom, a string, or an if/cond etc)
   - required? - optional; whether input is required (add red asterisk to label if so)
   - on-blur - optional; a function to call when the input loses focus
   - help - optional; a help message to display below the input"
  [& {:keys [label
             placeholder
             value
             on-change
             class
             disabled
             required?
             on-blur
             help]
      :or {label ""
           placeholder ""
           required? false
           class ""
           disabled false
           on-blur (fn [_] nil)
           help nil}}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label
     [:span label
      (when required?
        [:span.has-text-danger " *"])]]]
   [:div.field-body
    [:div.field
     [:div.control
      [:input.input {:type "text"
                     :placeholder placeholder
                     :value value
                     :on-change on-change
                     :on-blur on-blur
                     :disabled disabled
                     :class (if (and required? (str/blank? value))
                              "is-danger"
                              class)}]
      (when help
        [:p.help {:class (cond (and required? (str/blank? value)) "is-danger"
                               :else "")}
         (str/join " " [help
                        (when (and required? (str/blank? value))
                          "This field is required.")])])]]]])

(defn labelled-option-picker
  "A labelled horizontal form field, picking from a list of options.
   
   Takes a map with keys:
   - label - the label to display
   - value - the value source of the input
   - on-change - a function to call when the input changes
   - help - optional; a help message to display below the input
   - options - a vector of maps, each with keys:
     - label - the label to display for this option
     - value - the value to set when this option is selected.
   - icon - optional; an icon to display to the left of the select box"
  [& {:keys [label
             value
             on-change
             help
             options
             icon]}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label label]]
   [:div.field-body
    [:div.field
     [:div.control.has-icons-left
      [:div.select
       [:select
        {:value value
         :on-change on-change}
        (for [{:keys [label value]} options]
          [:option {:value value} label])]
       (when icon
         [:span.icon.is-small.is-left
          icon])]]
     (when help
       [:p.help help])]]])

(defn modal-lookup-picker
  "A component for triggering a modal to pick, eg, a newspaper or author
   and populate a target field with the result.
   
   Stores the search text in an atom, and filters the list of records
    based on the search text."
  [& {:keys
      [modal-id ;; id for registering the modal in the app db
       modal-title ;; title for the header of the modal
       records ;; records in the app db / an atom that the user can pick from
       on-pick-fn ;; function to call when the user picks a record
       display-field ;; primary field to display in the list of records (& filter matches against)
       help-field ;; optional; a secondary field to display in the list of records (& match against)
       ]}]
  (let [search-text (r/atom "")]
    (fn []
      [modal-button modal-id
       modal-title
       [:div
        (for [record @records]
          ;; filter records based on search text (case insensitive)
          ;; must match either display field or help field (if present)
          (let [match-text (str/join " " [(get record display-field "")
                                          (get record help-field "")])
                match? (re-find (re-pattern (str "(?i)" @search-text)) match-text)]
            (when match?
              [:div
               [:a.button.button
                {:class "is-fullwidth"
                 :on-click #(do
                              (rf/dispatch [:app/hide-modal modal-id])
                              (on-pick-fn record))}
                (get record display-field "")]
               [:p.help {:class (if match? "" "is-hidden")}
                (get record help-field "")]
               [:hr]])))]
       [:div
        [:div.field.is-fullwidth
         [:div.control
          [:input.input {:type "text"
                         :placeholder "Search"
                         :value @search-text
                         :on-change #(reset! search-text (-> % .-target .-value))}]]]]
       "is-info"])))


(defn labelled-trove-lookup
  "A component for looking up & retrieving records from trove (chapters or newspapers).
   
   Gives the user a text field to enter a trove_article_id or trove_newspaper_id,
   and a button to trigger a lookup.

   Lookup calls an API endpoint via an event (eg trove/get-chapter), which stores the
   result in the app db.

   Uses a self-contained atom to store its value; passes that value to the parent form
   when the user finds a successful match."
  [& {:keys [label ;; label to display for the input
             required? ;; whether the input is required
             record-type ;; type of record to lookup (eg 'chapter' or 'newspaper')
             placeholder ;; placeholder text to display in the input
             validation-regex ;; regex to validate the input against
             lookup-fn ;; function to call when the user clicks the lookup button
             help ;; help text to display below the input
             error ;; subscription to whether the lookup has errored
             loading? ;; subscription to whether the lookup is loading
             details ;; subscription to details of a successful lookup
             ]}]
  (r/with-let [value (r/atom "")
               id-target (if (= record-type "newspaper")
                           :trove_newspaper_id
                           :trove_article_id)]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label
       [:span label
        (when required?
          [:span.has-text-danger " *"])]]]
     [:div.field-body
      [:div.field
       [:div.field.has-addons
        [:div.control
         [:input.input {:type "text"
                        :placeholder placeholder
                        :value @value
                        :on-key-down #(when (= (.-key %) "Enter")
                                        (lookup-fn @value))
                        :on-blur #(lookup-fn @value)
                        :on-change #(reset! value (-> % .-target .-value))
                        :class (cond (and required? (str/blank? @value)) "is-danger"
                                     (not (re-matches validation-regex @value)) "is-danger"
                                     @error "is-danger"
                                     (and @details (= (str (get @details id-target)) @value)) "is-success"
                                     :else "is-info")}]]
        [:div.control
         [:button.button {:on-click #(lookup-fn @value)
                          :class
                          (str/join " " [(cond (and required? (str/blank? @value)) "is-danger"
                                               (not (re-matches validation-regex @value)) "is-danger"
                                               @error "is-danger"
                                               (and @details (= (str (get @details id-target)) @value)) "is-success"
                                               :else "is-info")
                                         (when @loading? "is-loading")])}
          (cond (and @details (= (str (get @details id-target)) @value)) [:span.icon [:i.material-icons "done"]]
                @error [:span.icon [:i.material-icons "error"]]
                :else [:span.icon [:i.material-icons "search"]])]]]
       [:p.help {:class (cond (and required? (str/blank? @value)) "is-danger"
                              @error "is-danger"
                              :else "")}
        (cond @error (str @error ". Please try again.")
              (and @details (= (str (get @details id-target)) @value)) (str "Found " record-type "!")
              :else (str help
                         (when (and required? (str/blank? @value)) " This field is required.")))]]]]))


(defn labelled-modal-picker
  "Complex component for triggering a modal to pick, eg, a newspaper or author
   and populate a target field with the result. Uses a label & a text field +
   modal-lookup-picker.

   Importantly, displays a *read only* text field with the selected record's
    display field value, and a hidden input with the selected record's id.
   That is, when selecting an author, the user sees the author's name in the
    text field, but the form is actually populated with the author's id."
  [& {:keys
      [label ;; the label to display
       required? ;; whether the field is required
       modal-id ;; id for registering the modal in the app db
       modal-title ;; title for the header of the modal
       records ;; records (in the app db) that the user can pick from
       on-pick-fn ;; function to call when the user picks a record
       display-field ;; primary field to display in the list of records (& filter matches against)
       help-field ;; optional; a secondary field to display in the list of records (& match against)
       value ;; the value source of the form field (not displayed!)
       placeholder ;; the placeholder text to display in the input
       help-text
       record-type ;; the type of record being picked (eg 'newspaper', 'author')
       ]}]
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label
     [:span label
      (when required?
        [:span.has-text-danger " *"])]]]
   [:div.field-body
    [:div.field
     [:div.field.has-addons
      [:div.control
       [:input.input {:type "text"
                      :disabled true
                      :value value
                      :class (and required? (str/blank? value) "is-danger")
                      :placeholder placeholder}]]
      [modal-lookup-picker
       {:modal-id modal-id
        :modal-title modal-title
        :records records
        :on-pick-fn on-pick-fn
        :display-field display-field
        :help-field help-field}]
      (when value
        [:a.button.button
         {:class "is-ghost"
          :href (str "#/" record-type "/" value)
          :target "_blank"}
         [:span 
          (first (map #(get % display-field "") (filter #(= (:id %) value) @records)))
          ]])]
     [:p.help {:class (if (and required? (str/blank? value)) "is-danger" "")}
      (str help-text
           (when (and required? (str/blank? value)) " This field is required."))]]]])


(defn create-button
  "A button for creating a record (placed at the bottom of a form.)
   
   Takes:
   - text - the text to display on the button
   - on-click - a function to call when the button is clicked
   - error-help - help text to display below the button if there's an error
   - success-help - help text to display below the button if the creation is successful
   - loading? - an atom containing whether the button is loading
   - disabled - optional; when to disable the button (can be an atom, a string, or an if/cond etc)
   - success - atom containing details of the successful creation (if any)
   - error - details of the error (if any)
   - success-link - optional; a link to display when the creation is successful"
  [& {:keys [text
             on-click
             error-help
             success-help
             loading?
             disabled
             success
             error
             success-link]
      :or {disabled false
           success-link nil}}]
  (if-not @success
    [:div.field
     [:a.button.button {:class (str/join " " [(if @error "is-danger" "is-info")
                                              (when @loading? "is-loading")])
                        :disabled (or @loading? disabled)
                        :on-click on-click}
      [:span text]
      (if @error
        [:span.icon [:i.material-icons "error"]]
        [:span.icon [:i.material-icons "add"]])]
     [:p.help {:class (if @error "is-danger" "")}
      (if @error error-help "")]]
    [:div.field
     [:a.button.button {:class "is-success"
                        :href success-link}
      [:span "Record Created!"]
      [:span.icon [:i.material-icons "done"]]]
     [:p.help {:class "is-success"} success-help]]))

(defn edit-button
  "A button for editing a record (placed at the bottom of a form.)
   
   Takes:
   - text - the text to display on the button
   - on-click - a function to call when the button is clicked
   - error-help - help text to display below the button if there's an error
   - success-help - help text to display below the button if the creation is successful
   - loading? - an atom containing whether the button is loading
   - disabled - optional; when to disable the button (can be an atom, a string, or an if/cond etc)
   - success - atom containing details of the successful creation (if any)
   - error - details of the error (if any)
   - success-link - optional; a link to display when the creation is successful"
  [& {:keys [text
             on-click
             error-help
             success-help
             loading?
             disabled
             success
             error
             success-link]
      :or {disabled false
           success-link nil}}]
  (if-not @success
    [:div.field
     [:a.button.button {:class (str/join " " [(cond @success "is-success"
                                                    @error "is-danger"
                                                    :else "is-info")
                                              (when @loading? "is-loading")])
                        :disabled (or @loading? disabled)
                        :on-click on-click}
      [:span text]
      (if @error
        [:span.icon [:i.material-icons "error"]]
        [:span.icon [:i.material-icons "import_export"]])]
     [:p.help {:class (if @error "is-danger" "")}
      (if @error error-help "")]]
    [:div.field
     [:a.button.button {:class "is-success"
                        :href success-link}
      [:span "Record Updated!"]
      [:span.icon [:i.material-icons "done"]]]
     [:p.help {:class "is-success"} success-help]]))