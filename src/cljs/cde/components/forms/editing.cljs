(ns cde.components.forms.editing
  "Editing forms for author, chapter, newspaper, and title records.

  Uses the generic form components from [[cde.components.forms]] to build
  multi-tab editing forms with pre-populated field values.

  Key components:
  - [[edit-author-form]] - Author editing with name, nationality, and details tabs
  - [[edit-chapter-form]] - Chapter editing with title, number, and date fields
  - [[edit-newspaper-form]] - Newspaper editing with key details and location tabs
  - [[edit-title-form]] - Title editing with details, notes, and danger zone tabs

  See also: [[cde.components.forms.creation]] for creation forms."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]
   [cde.components.forms :as forms]
   [cde.utils :refer [key->help key->title key->placeholder]]))

(defn- ^:no-doc gender-option-picker
  "Dropdown select for editing author gender field.

  Arguments:
  - `form-details` - subscription to current form state
  - `updating?` - subscription to update loading state"
  [form-details updating?]
  (forms/labelled-option-picker
   {:label (key->title :gender :author)
    :value (:gender @form-details)
    :on-change #(rf/dispatch [:author/update-edit-author-form-field :gender (-> % .-target .-value)])
    :icon [:i.material-icons "transgender"]
    :help (key->help :gender :author)
    :options [{:value "" :label "Author Gender"}
              {:value "Male" :label "Male"}
              {:value "Female" :label "Female"}
              {:value "Unknown" :label "Unknown"}
              {:value "Multiple" :label "Multiple"}
              {:value "Both" :label "Both"}]}))

(defn edit-author-form
  "Multi-tab form for editing an existing author's metadata.

  Tabs: Name (common name, other names), Nationality (nationality, details),
  Other (gender, author details source).

  Required field: common_name."
  []
  (r/with-let [form-details (rf/subscribe [:author/edit-author-form])
               updating? (rf/subscribe [:author/update-loading?])
               update-success (rf/subscribe [:author/update-success])
               update-error (rf/subscribe [:author/update-error])
               active-tab (r/atom 0)]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/edit-button
                  {:text "Update Author"
                   :on-click #(rf/dispatch [:author/update-author @form-details])
                   :success-help "Author updated successfully!"
                   :error-help "Error updating author. Please try again."
                   :disabled (str/blank? (:common_name @form-details))
                   :loading? updating?
                   :success update-success
                   :error update-error
                   :success-link (str "#/author/" (:id @form-details))})
         :visible-tab active-tab
         :tabs [{:tab-title "Name"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :common_name :author)
                             :placeholder (key->placeholder :common_name :author)
                             :value (:common_name @form-details)
                             :on-change #(rf/dispatch [:author/update-edit-author-form-field :common_name (-> % .-target .-value)])
                             :required? true
                             :help (key->help :common_name :author)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :other_name :author)
                             :placeholder (key->placeholder :other_name :author)
                             :value (:other_name @form-details)
                             :on-change #(rf/dispatch [:author/update-edit-author-form-field :other_name (-> % .-target .-value)])
                             :help (key->help :other_name :author)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")}))}
                {:tab-title "Nationality"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :nationality :author)
                             :placeholder (key->placeholder :nationality :author)
                             :value (:nationality @form-details)
                             :on-change #(rf/dispatch [:author/update-edit-author-form-field :nationality (-> % .-target .-value)])
                             :help (key->help :nationality :author)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :nationality_details :author)
                             :placeholder (key->placeholder :nationality_details :author)
                             :value (:nationality_details @form-details)
                             :on-change #(rf/dispatch [:author/update-edit-author-form-field :nationality_details (-> % .-target .-value)])
                             :help (key->help :nationality_details :author)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")}))}
                {:tab-title "Other"
                 :content (forms/simple-ff-block
                           [gender-option-picker form-details updating?]
                           (forms/labelled-text-field
                            {:label (key->title :author_details :author)
                             :placeholder (key->placeholder :author_details :author)
                             :value (:author_details @form-details)
                             :on-change #(rf/dispatch [:author/update-edit-author-form-field :author_details (-> % .-target .-value)])
                             :help (key->help :author_details :author)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")}))}]})])))

(defn edit-title-form
  "Multi-tab form for editing an existing title's metadata.

  Tabs: Key Details (titles, length), Extra Notes (attribution, sources),
  Danger Zone (change author/newspaper - use with caution)."
  []
  (r/with-let [form-details (rf/subscribe [:title/edit-title-form])
               updating (rf/subscribe [:title/update-loading?])
               update-success (rf/subscribe [:title/update-success])
               update-error (rf/subscribe [:title/update-error])
               active-tab (r/atom 0)
               author-list (rf/subscribe [:platform/all-authors])
               newspaper-list (rf/subscribe [:platform/all-newspapers])]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/edit-button
                  {:text "Edit Title"
                   :on-click #(rf/dispatch [:title/update-title @form-details])
                   :success-help "Title updated successfully! Click to see it in the database."
                   :error-help "Error updating title. Please try again."
                   :disabled (or (str/blank? (:publication_title @form-details))
                                 (str/blank? (:newspaper_table_id @form-details))
                                 (str/blank? (:author_id @form-details)))
                   :loading? updating
                   :success update-success
                   :error update-error
                   :success-link (str "#/title/" (:id @update-success))})
         :visible-tab active-tab
         :tabs [{:tab-title "Key Details"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :publication_title :title)
                             :placeholder (key->placeholder :publication_title :title)
                             :value (:publication_title @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :publication_title (-> % .-target .-value)])
                             :required? true
                             :help (key->help :publication_title :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :common_title :title)
                             :value (:common_title @form-details)
                             :placeholder (key->placeholder :common_title :title)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :common_title (-> % .-target .-value)])
                             :help (key->help :common_title :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-option-picker
                            {:label (key->title :length :title)
                             :value (:length @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :length (-> % .-target .-value)])
                             :icon [:i.material-icons "auto_stories"]
                             :help (key->help :length :title)
                             :options [{:value nil :label "Unknown"}
                                       {:value 0 :label "Serialised Title"}
                                       {:value 1 :label "Short Single Edition"}
                                       {:value 8 :label "10,000+ Words (in a Single Edition)"}]})
                           (forms/labelled-text-field
                            {:label (key->title :attributed_author_name :title)
                             :placeholder (key->placeholder :attributed_author_name :title)
                             :value (:attributed_author_name @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :attributed_author_name (-> % .-target .-value)])
                             :help (key->help :attributed_author_name :title)
                             :disabled @updating}))}
                {:tab-title "Extra Notes"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :name_category :title)
                             :placeholder (key->placeholder :name_category :title)
                             :value (:name_category @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :name_category (-> % .-target .-value)])
                             :help (key->help :name_category :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :author_of :title)
                             :placeholder (key->placeholder :author_of :title)
                             :value (:author_of @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :author_of (-> % .-target .-value)])
                             :help (key->help :author_of :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :inscribed_author_nationality :title)
                             :placeholder (key->placeholder :inscribed_author_nationality :title)
                             :value (:inscribed_author_nationality @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :inscribed_author_nationality (-> % .-target .-value)])
                             :help (key->help :inscribed_author_nationality :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :inscribed_author_gender :title)
                             :placeholder (key->placeholder :inscribed_author_gender :title)
                             :value (:inscribed_author_gender @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :inscribed_author_gender (-> % .-target .-value)])
                             :help (key->help :inscribed_author_gender :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :information_source :title)
                             :placeholder (key->placeholder :information_source :title)
                             :value (:information_source @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :information_source (-> % .-target .-value)])
                             :help (key->help :information_source :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :additional_info :title)
                             :placeholder (key->placeholder :additional_info :title)
                             :value (:additional_info @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :additional_info (-> % .-target .-value)])
                             :help (key->help :additional_info :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :also_published :title)
                             :placeholder (key->placeholder :also_published :title)
                             :value (:also_published @form-details)
                             :on-change #(rf/dispatch [:title/update-edit-title-form-field :also_published (-> % .-target .-value)])
                             :help (key->help :also_published :title)
                             :disabled @updating
                             :class (if @updating "is-static" "")}))}
                {:tab-title "Danger Zone"
                 :content (forms/simple-ff-block
                           (forms/labelled-modal-picker
                            {:label "Author"
                             :required? true
                             :placeholder "Find an author..."
                             :help-text (key->help :author_id :title)
                             :modal-id :lookup-author-modal
                             :modal-title "Pick An Author"
                             :records author-list
                             :record-type "author"
                             :on-pick-fn (fn [author] (rf/dispatch [:title/update-edit-title-form-field :author_id (:id author)]))
                             :display-field :common_name
                             :help-field :other_name
                             :value (:author_id @form-details)})
                           (forms/labelled-modal-picker
                            {:label "Newspaper"
                             :required? true
                             :placeholder "Find a newspaper..."
                             :help-text (key->help :newspaper_table_id :title)
                             :modal-id :lookup-newspaper-modal
                             :modal-title "Find A Newspaper"
                             :records newspaper-list
                             :record-type "newspaper"
                             :on-pick-fn (fn [newspaper] (rf/dispatch [:title/update-edit-title-form-field :newspaper_table_id (:id newspaper)]))
                             :display-field :title
                             :help-field :common_title
                             :value (:newspaper_table_id @form-details)}))}]})])))

(defn edit-chapter-form
  "Multi-tab form for editing an existing chapter's metadata.

  Tabs: Key Details (title, number), Extra Notes (publication date)."
  []
  (r/with-let [form-details (rf/subscribe [:chapter/edit-chapter-form])
               updating (rf/subscribe [:chapter/update-loading?])
               update-success (rf/subscribe [:chapter/update-success])
               update-error (rf/subscribe [:chapter/update-error])
               active-tab (r/atom 0)]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/edit-button
                  {:text "Edit Chapter"
                   :on-click #(rf/dispatch [:chapter/update-chapter @form-details])
                   :success-help "Chapter updated successfully! Click to see it in the database."
                   :error-help "Error updating chapter. Please try again."
                   :disabled (or (str/blank? (:chapter_title @form-details))
                                 (str/blank? (:title_id @form-details)))
                   :loading? updating
                   :success update-success
                   :error update-error
                   :success-link (str "#/chapter/" (:id @update-success))})
         :visible-tab active-tab
         :tabs [{:tab-title "Key Details"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :chapter_title :chapter)
                             :placeholder (key->placeholder :chapter_title :chapter)
                             :value (:chapter_title @form-details)
                             :on-change #(rf/dispatch [:chapter/update-edit-chapter-form-field :chapter_title (-> % .-target .-value)])
                             :required? true
                             :help (key->help :chapter_title :chapter)
                             :disabled @updating
                             :class (if @updating "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :chapter_number :chapter)
                             :value (:chapter_number @form-details)
                             :placeholder (key->placeholder :chapter_number :chapter)
                             :on-change #(rf/dispatch [:chapter/update-edit-chapter-form-field :chapter_number (-> % .-target .-value)])
                             :help (key->help :chapter_number :chapter)
                             :disabled @updating
                             :class (if @updating "is-static" "")}))}
                {:tab-title "Extra Notes"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :final_date :chapter)
                             :placeholder (key->placeholder :final_date :chapter)
                             :value (:final_date @form-details)
                             :on-change #(rf/dispatch [:chapter/update-edit-chapter-form-field :final_date (-> % .-target .-value)])
                             :help (key->help :final_date :chapter)
                             :disabled @updating
                             :class (if @updating "is-static" "")
                             :validation #(or (nil? %) (str/blank? %) (re-matches #"^\d{4}-\d{2}-\d{2}" %))}))}]})])))

(defn edit-newspaper-form
  "Multi-tab form for editing an existing newspaper's metadata.

  Tabs: Key Details (title, common title), Location & Type (colony/state, type, location),
  Additional Info (start/end year, ISSN, details).

  Required field: title."
  []
  (r/with-let [form-details (rf/subscribe [:newspaper/edit-newspaper-form])
               updating? (rf/subscribe [:newspaper/update-loading?])
               update-success (rf/subscribe [:newspaper/update-success])
               update-error (rf/subscribe [:newspaper/update-error])
               active-tab (r/atom 0)]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/edit-button
                  {:text "Update Newspaper"
                   :on-click #(rf/dispatch [:newspaper/update-newspaper @form-details])
                   :success-help "Newspaper updated successfully!"
                   :error-help "Error updating newspaper. Please try again."
                   :disabled (str/blank? (:title @form-details))
                   :loading? updating?
                   :success update-success
                   :error update-error
                   :success-link (str "#/newspaper/" (:id @form-details))})
         :visible-tab active-tab
         :tabs [{:tab-title "Key Details"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :title :newspaper)
                             :placeholder (key->placeholder :title :newspaper)
                             :value (:title @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :title (-> % .-target .-value)])
                             :required? true
                             :help (key->help :title :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :common_title :newspaper)
                             :placeholder (key->placeholder :common_title :newspaper)
                             :value (:common_title @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :common_title (-> % .-target .-value)])
                             :help (key->help :common_title :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")}))}
                {:tab-title "Location & Type"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :colony_state :newspaper)
                             :placeholder (key->placeholder :colony_state :newspaper)
                             :value (:colony_state @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :colony_state (-> % .-target .-value)])
                             :help (key->help :colony_state :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :newspaper_type :newspaper)
                             :placeholder (key->placeholder :newspaper_type :newspaper)
                             :value (:newspaper_type @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :newspaper_type (-> % .-target .-value)])
                             :help (key->help :newspaper_type :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :location :newspaper)
                             :placeholder (key->placeholder :location :newspaper)
                             :value (:location @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :location (-> % .-target .-value)])
                             :help (key->help :location :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")}))}
                {:tab-title "Additional Info"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :start_year :newspaper)
                             :placeholder (key->placeholder :start_year :newspaper)
                             :value (:start_year @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :start_year (-> % .-target .-value js/parseInt)])
                             :help (key->help :start_year :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :end_year :newspaper)
                             :placeholder (key->placeholder :end_year :newspaper)
                             :value (:end_year @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :end_year (-> % .-target .-value js/parseInt)])
                             :help (key->help :end_year :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :issn :newspaper)
                             :placeholder (key->placeholder :issn :newspaper)
                             :value (:issn @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :issn (-> % .-target .-value)])
                             :help (key->help :issn :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :details :newspaper)
                             :placeholder (key->placeholder :details :newspaper)
                             :value (:details @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-edit-newspaper-form-field :details (-> % .-target .-value)])
                             :help (key->help :details :newspaper)
                             :disabled @updating?
                             :class (if @updating? "is-static" "")}))}]})])))
