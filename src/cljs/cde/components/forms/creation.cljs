(ns cde.components.forms.creation
  "Creation forms for author, chapter, newspaper, and title records.

  Uses the generic form components from [[cde.components.forms]] to build
  multi-tab creation forms with Trove lookup integration.

  Key components:
  - [[new-author-form]] - Author creation with name, nationality, and details tabs
  - [[new-chapter-form]] - Chapter creation with Trove article ID lookup
  - [[new-newspaper-form]] - Newspaper creation with Trove newspaper ID lookup
  - [[new-title-form]] - Title creation with author/newspaper selection

  See also: [[cde.components.forms.editing]] for editing forms."
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]
   [cde.components.forms :as forms]
   [cde.utils :refer [key->help key->title key->placeholder]]))

(defn new-author-form
  "Multi-tab form for creating a new author record.

  Tabs: Name (common name, other names), Nationality (nationality, details),
  Other (gender, author details source).

  Required field: common_name."
  []
  (r/with-let [form-details (rf/subscribe [:author/new-author-form])
               creating? (rf/subscribe [:author/creation-loading?])
               create-success (rf/subscribe [:author/creation-success])
               create-error (rf/subscribe [:author/creation-error])
               active-tab (r/atom 0)]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/create-button
                  {:text "Create Author"
                   :on-click #(rf/dispatch [:author/create-new-author @form-details])
                   :success-help "Author created successfully! Click to see it in the database."
                   :error-help "Error creating author. Please try again."
                   :disabled (str/blank? (:common_name @form-details))
                   :loading? creating?
                   :success create-success
                   :error create-error
                   :success-link (str "#/author/" (:id @create-success))})
         :visible-tab active-tab
         :tabs [{:tab-title "Name"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :common_name :author)
                             :placeholder (key->placeholder :common_name :author)
                             :value (:common_name @form-details)
                             :on-change #(rf/dispatch [:author/update-new-author-form-field :common_name (-> % .-target .-value)])
                             :required? true
                             :help (key->help :common_name :author)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :other_name :author)
                             :placeholder (key->placeholder :other_name :author)
                             :value (:other_name @form-details)
                             :on-change #(rf/dispatch [:author/update-new-author-form-field :other_name (-> % .-target .-value)])
                             :help (key->help :other_name :author)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")}))}
                {:tab-title "Nationality"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :nationality :author)
                             :placeholder (key->placeholder :nationality :author)
                             :value (:nationality @form-details)
                             :on-change #(rf/dispatch [:author/update-new-author-form-field :nationality (-> % .-target .-value)])
                             :help (key->help :nationality :author)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :nationality_details :author)
                             :placeholder (key->placeholder :nationality_details :author)
                             :value (:nationality_details @form-details)
                             :on-change #(rf/dispatch [:author/update-new-author-form-field :nationality_details (-> % .-target .-value)])
                             :help (key->help :nationality_details :author)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")}))}
                {:tab-title "Other"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :author_details :author)
                             :placeholder (key->placeholder :author_details :author)
                             :value (:author_details @form-details)
                             :on-change #(rf/dispatch [:author/update-new-author-form-field :author_details (-> % .-target .-value)])
                             :help (key->help :author_details :author)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")}))}]})])))

(defn- ^:no-doc trove-article-id-field
  "Input field for entering and validating a Trove article ID.

  Performs API lookup to verify the article exists in Trove and checks
  for duplicates against existing chapters in the database.

  Arguments:
  - `form-details` - subscription to current form state
  - `trove-loading?` - subscription to Trove API loading state
  - `trove-error` - subscription to Trove API error state
  - `trove-details` - subscription to Trove API response data
  - `existing-ids` - subscription to list of existing chapter Trove article IDs"
  [form-details trove-loading? trove-error trove-details existing-ids]
  (let [value (:trove_article_id @form-details)
        is-valid-format? (and (not (str/blank? (str value)))
                              (re-matches #"\d+" (str value)))
        is-duplicate? (some #(= value (str %)) @existing-ids)
        is-found? (and (false? @trove-loading?)
                       (not (nil? @trove-details))
                       (= value (str (:trove_article_id @trove-details))))
        input-class (cond
                      (str/blank? (str value)) ""
                      (not is-valid-format?) "is-danger"
                      (and @trove-error (string? @trove-error)) "is-danger"
                      is-duplicate? "is-danger"
                      is-found? "is-success"
                      :else "is-info")]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Trove Article ID"]]
     [:div.field-body
      [:div.field
       [:div.field.has-addons
        [:div.control
         [:input.input {:type "text"
                        :class (str input-class (when @trove-loading? " is-loading"))
                        :placeholder "12345678"
                        :value value
                        :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :trove_article_id (-> % .-target .-value)])}]]
        [:div.control
         [:button.button {:class (str input-class (when @trove-loading? " is-loading"))
                          :on-click #(rf/dispatch [:trove/get-chapter value])}
          (if @trove-details
            [:span "Update Details from Trove"]
            [:span "Get Details from Trove"])]]]
       [:p.help {:class (cond
                          @trove-loading? "is-info"
                          (and (not (str/blank? (str value))) (not is-valid-format?)) "is-danger"
                          (and @trove-error (string? @trove-error)) "is-danger"
                          is-duplicate? "is-danger"
                          is-found? "is-success"
                          :else "")}
        (cond
          @trove-loading? "Checking Trove..."
          (and (not (str/blank? (str value))) (not is-valid-format?)) "Trove ID must be a number."
          (and @trove-error (string? @trove-error)) @trove-error
          is-duplicate? "A chapter with this Trove ID is already in the database!"
          is-found? "We found this chapter in Trove!"
          :else "This is the 'Article ID' (from Trove) for the chapter you're adding.")]]]]))

(defn- ^:no-doc title-id-field
  "Input field for entering and validating a title ID.

  Performs API lookup to verify the title exists in our database.

  Arguments:
  - `form-details` - subscription to current form state
  - `title-loading?` - subscription to title API loading state
  - `title-error` - subscription to title API error state
  - `title-details` - subscription to title API response data"
  [form-details title-loading? title-error title-details]
  (let [value (:title_id @form-details)
        is-valid-format? (and (not (str/blank? (str value)))
                              (re-matches #"\d+" (str value)))
        is-found? (and (false? @title-loading?)
                       (not (nil? @title-details))
                       (= value (str (:id @title-details))))
        input-class (cond
                      (str/blank? (str value)) ""
                      (not is-valid-format?) "is-danger"
                      (and @title-error (string? @title-error)) "is-danger"
                      is-found? "is-success"
                      :else "is-info")]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Title ID"]]
     [:div.field-body
      [:div.field
       [:div.field.has-addons
        [:div.control
         [:input.input {:type "text"
                        :class (str input-class (when @title-loading? " is-loading"))
                        :placeholder "1234"
                        :value value
                        :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :title_id (-> % .-target .-value)])}]]
        [:div.control
         [:button.button {:class (str input-class (when @title-loading? " is-loading"))
                          :on-click #(rf/dispatch [:title/get-title value])}
          [:span "Check Title Record"]]]]
       [:p.help {:class (cond
                          @title-loading? "is-info"
                          (and (not (str/blank? (str value))) (not is-valid-format?)) "is-danger"
                          (and @title-error (string? @title-error)) "is-danger"
                          is-found? "is-success"
                          :else "")}
        (cond
          @title-loading? "Checking Title..."
          (and (not (str/blank? (str value))) (not is-valid-format?)) "Title ID must be a number."
          (and @title-error (string? @title-error) (not (str/includes? @title-error "Unknown")))
          @title-error
          (and @title-error (string? @title-error))
          (str @title-error " - please check the Title ID and try again.")
          is-found? "The title you're adding a chapter to exists in our database!"
          :else "This is the 'Title ID' (from our database) for the title/story to which you're adding the chapter.")]]]]))

(defn- ^:no-doc chapter-details-section
  "Form section for chapter details after Trove lookup succeeds.

  Shows editable fields for chapter title, number, and publication date.

  Arguments:
  - `form-details` - subscription to current form state"
  [form-details]
  [:div.block
   [:h3 {:style {:text-align "center"}} "Chapter Details"]
   (forms/labelled-text-field
    {:label "Chapter Title"
     :placeholder "The Chapter Title"
     :value (:chapter_title @form-details)
     :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :chapter_title (-> % .-target .-value)])
     :help "This is the chapter title for the chapter you're adding."})
   (forms/labelled-text-field
    {:label "Chapter Number"
     :placeholder "Chapter Number (eg 'XII')"
     :value (:chapter_number @form-details)
     :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :chapter_number (-> % .-target .-value)])
     :help "This is the chapter number for the chapter you're adding. This is usually a roman numeral (eg 'XII')."})
   (forms/labelled-text-field
    {:label "Publication Date"
     :placeholder "Publication Date in YYYY-MM-DD format (eg '2018-01-01')"
     :value (:final_date @form-details)
     :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :final_date (-> % .-target .-value)])
     :help "This is the publication date for the chapter. This is usually the date of the newspaper issue in which the chapter was published."
     :validation #(or (str/blank? %) (re-matches #"\d{4}-\d{2}-\d{2}" %))})])

(defn new-chapter-form
  "Form for creating a new chapter in an existing title.

  Two-phase form: first validates Trove article ID and title ID,
  then shows chapter details fields (title, number, date) for confirmation.

  This form has a unique validation flow where both the Trove article
  and the parent title must be verified before chapter details can be edited."
  []
  (r/with-let [form-details (rf/subscribe [:chapter/new-chapter-form])
               trove-loading? (rf/subscribe [:trove/loading?])
               trove-error (rf/subscribe [:trove/error])
               trove-details (rf/subscribe [:trove/details])
               title-loading? (rf/subscribe [:title/metadata-loading?])
               title-error (rf/subscribe [:title/error])
               title-details (rf/subscribe [:title/details])
               existing-chapter-trove-article-ids (rf/subscribe [:trove/chapter-exists-list])
               creation-loading? (rf/subscribe [:chapter/creation-loading?])
               creation-error (rf/subscribe [:chapter/creation-error])
               created-chapter-details (rf/subscribe [:chapter/creation-success])]
    (fn []
      (let [title-valid? (= (:title_id @form-details) (str (:id @title-details)))
            trove-valid? (= (:trove_article_id @form-details) (str (:trove_article_id @trove-details)))
            not-duplicate? (not (some #(= (:trove_article_id @form-details) (str %))
                                      @existing-chapter-trove-article-ids))
            all-lookups-valid? (and title-valid? trove-valid? not-duplicate?)
            details-populated? (or (not (nil? (:chapter_title @form-details)))
                                   (not (nil? (:chapter_number @form-details))))]
        [:div
         ;; Phase 1: Trove article ID and Title ID validation
         [:div.block
          [trove-article-id-field form-details trove-loading? trove-error
           trove-details existing-chapter-trove-article-ids]
          [title-id-field form-details title-loading? title-error title-details]]

         ;; Submit/Check button (only shown when lookups are valid)
         (when all-lookups-valid?
           [:div.block.has-text-centered
            (cond
              ;; Success - navigate to created chapter
              @created-chapter-details
              [:a.button.button {:class "is-success"
                                 :href (str "#/chapter/" (:id @created-chapter-details))}
               [:span "Chapter Created! Go to Chapter"]]

              ;; Error - allow retry
              @creation-error
              [:button.button {:class "is-danger"
                               :on-click #(rf/dispatch [:chapter/create-new-chapter @form-details])}
               [:span "Submit"]]

              ;; Details not yet populated - show check button
              (not details-populated?)
              [:button.button {:on-click #(rf/dispatch [:chapter/populate-new-chapter-form])}
               [:span "Check Details"]]

              ;; Ready to submit
              :else
              [:button.button {:class (if @creation-loading? "is-loading" "is-info")
                               :on-click #(rf/dispatch [:chapter/create-new-chapter @form-details])}
               [:span "Submit"]])])

         ;; Phase 2: Chapter details (shown after Check Details is clicked)
         (when details-populated?
           [chapter-details-section form-details])]))))

(defn new-newspaper-form
  "Multi-tab form for creating a new newspaper record.

  Tabs: Key Details (Trove ID, title), Extra Notes (location, type, details).
  Integrates with Trove API to lookup newspaper by trove_newspaper_id."
  []
  (r/with-let [form-details (rf/subscribe [:newspaper/new-newspaper-form])
               creating? (rf/subscribe [:newspaper/creation-loading?])
               create-success (rf/subscribe [:newspaper/creation-success])
               create-error (rf/subscribe [:newspaper/creation-error])
               active-tab (r/atom 0)]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/create-button
                  {:text "Create Newspaper"
                   :on-click #(rf/dispatch [:newspaper/create-new-newspaper @form-details])
                   :success-help "Newspaper created successfully! Click to see it in the database."
                   :error-help "Error creating newspaper. Please try again."
                   :disabled (or (str/blank? (:trove_newspaper_id @form-details))
                                 (str/blank? (:title @form-details)))
                   :loading? creating?
                   :success create-success
                   :error create-error
                   :success-link (str "#/newspaper/" (:id @create-success))})
         :visible-tab active-tab
         :tabs [{:tab-title "Key Details"
                 :content (forms/simple-ff-block
                           (forms/labelled-trove-lookup
                            {:label (key->title :trove_newspaper_id :newspaper)
                             :required? true
                             :record-type "newspaper"
                             :placeholder "1492"
                             :validation-regex #"\d+"
                             :lookup-fn (fn [value]
                                          (do
                                            (rf/dispatch [:newspaper/update-new-newspaper-form-field :trove_newspaper_id value])
                                            (rf/dispatch [:trove/get-newspaper value])))
                             :help (key->help :trove_newspaper_id :newspaper)
                             :error (rf/subscribe [:trove/error])
                             :loading? (rf/subscribe [:trove/loading?])
                             :details (rf/subscribe [:trove/details])})
                           (forms/labelled-text-field
                            {:label (key->title :title :newspaper)
                             :placeholder (key->placeholder :title :newspaper)
                             :value (:title @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :title (-> % .-target .-value)])
                             :required? true
                             :help (key->help :title :newspaper)
                             :disabled (or @creating? (str/blank? (:trove_newspaper_id @form-details)))
                             :class (if @creating? "is-static" "")}))}
                {:tab-title "Extra Notes"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :colony_state :newspaper)
                             :placeholder (key->placeholder :colony_state :newspaper)
                             :value (:colony_state @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :colony_state (-> % .-target .-value)])
                             :help (key->help :colony_state :newspaper)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :newspaper_type :newspaper)
                             :placeholder (key->placeholder :newspaper_type :newspaper)
                             :value (:newspaper_type @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :newspaper_type (-> % .-target .-value)])
                             :help (key->help :newspaper_type :newspaper)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :location :newspaper)
                             :placeholder (key->placeholder :location :newspaper)
                             :value (:location @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :location (-> % .-target .-value)])
                             :help (key->help :location :newspaper)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                          ;;  (forms/labelled-text-field
                          ;;   {:label (key->title :start_date :newspaper)
                          ;;    :placeholder (key->placeholder :start_date :newspaper)
                          ;;    :value (:start_date @form-details)
                          ;;    :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :start_date (-> % .-target .-value)])
                          ;;    :help (key->help :start_date :newspaper)
                          ;;    :disabled @creating?
                          ;;    :class (if @creating? "is-static" "")})
                          ;;  (forms/labelled-text-field
                          ;;   {:label (key->title :end_date :newspaper)
                          ;;    :placeholder (key->placeholder :end_date :newspaper)
                          ;;    :value (:end_date @form-details)
                          ;;    :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :end_date (-> % .-target .-value)])
                          ;;    :help (key->help :end_date :newspaper)
                          ;;    :disabled @creating?
                          ;;    :class (if @creating? "is-static" "")})
                          ;;  (forms/labelled-text-field
                          ;;   {:label (key->title :start_year :newspaper)
                          ;;    :placeholder (key->placeholder :start_year :newspaper)
                          ;;    :value (:start_year @form-details)
                          ;;    :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :start_year (-> % .-target .-value)])
                          ;;    :help (key->help :start_year :newspaper)
                          ;;    :disabled @creating?
                          ;;    :class (if @creating? "is-static" "")})
                          ;;  (forms/labelled-text-field
                          ;;   {:label (key->title :end_year :newspaper)
                          ;;    :placeholder (key->placeholder :end_year :newspaper)
                          ;;    :value (:end_year @form-details)
                          ;;    :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :end_year (-> % .-target .-value)])
                          ;;    :help (key->help :end_year :newspaper)
                          ;;    :disabled @creating?
                          ;;    :class (if @creating? "is-static" "")})
                          ;;  (forms/labelled-text-field
                          ;;   {:label (key->title :issn :newspaper)
                          ;;    :placeholder (key->placeholder :issn :newspaper)
                          ;;    :value (:issn @form-details)
                          ;;    :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :issn (-> % .-target .-value)])
                          ;;    :help (key->help :issn :newspaper)
                          ;;    :disabled @creating?
                          ;;    :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :details :newspaper)
                             :placeholder (key->placeholder :details :newspaper)
                             :value (:details @form-details)
                             :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :details (-> % .-target .-value)])
                             :help (key->help :details :newspaper)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                          ;;  (forms/labelled-text-field
                          ;;   {:label (key->title :common_title :newspaper)
                          ;;    :placeholder (key->placeholder :common_title :newspaper)
                          ;;    :value (:common_title @form-details)
                          ;;    :on-change #(rf/dispatch [:newspaper/update-new-newspaper-form-field :common_title (-> % .-target .-value)])
                          ;;    :help (key->help :common_title :newspaper)
                          ;;    :disabled @creating?
                          ;;    :class (if @creating? "is-static" "")})
                           )}]})])))
(defn new-title-form
  "Multi-tab form for creating a new title (serialised fiction work).

  Tabs: Key Details (author, newspaper, title names), Extra Notes (attribution, sources).
  Uses modal pickers for selecting author and newspaper records."
  []
  (r/with-let [form-details (rf/subscribe [:title/new-title-form])
               creating? (rf/subscribe [:title/creation-loading?])
               create-success (rf/subscribe [:title/creation-success])
               create-error (rf/subscribe [:title/creation-error])
               active-tab (r/atom 0)
               author-list (rf/subscribe [:platform/all-authors])
               newspaper-list (rf/subscribe [:platform/all-newspapers])]
    (fn []
      [:div
       (forms/tabbed-form
        {:footer (forms/create-button
                  {:text "Create Title"
                   :on-click #(rf/dispatch [:title/create-new-title @form-details])
                   :success-help "Title created successfully! Click to see it in the database."
                   :error-help "Error creating title. Please try again."
                   :disabled (or (str/blank? (:publication_title @form-details))
                                 (str/blank? (:newspaper_table_id @form-details))
                                 (str/blank? (:author_id @form-details)))
                   :loading? creating?
                   :success create-success
                   :error create-error
                   :success-link (str "#/title/" (:id @create-success))})
         :visible-tab active-tab
         :tabs [{:tab-title "Key Details"
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
                             :on-pick-fn (fn [author] (rf/dispatch [:title/update-new-title-form-field :author_id (:id author)]))
                             :display-field :common_name
                             :help-field :other_name
                             :value (:author_id @form-details)})
                           (forms/labelled-modal-picker
                            {:label "Newspaper"
                             :required? true
                             :placeholder "Find a newspaper..."
                             :help-text (key->help :newspaper_table_id :title)
                             :modal-id :lookup-newspaper-modal
                             :modal-title "Pick A Newspaper"
                             :records newspaper-list
                             :record-type "newspaper"
                             :on-pick-fn (fn [newspaper] (rf/dispatch [:title/update-new-title-form-field :newspaper_table_id (:id newspaper)]))
                             :display-field :title
                             :help-field :common_title
                             :value (:newspaper_table_id @form-details)})
                           (forms/labelled-text-field
                            {:label (key->title :publication_title :title)
                             :placeholder (key->placeholder :publication_title :title)
                             :value (:publication_title @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :publication_title (-> % .-target .-value)])
                             :required? true
                             :help (key->help :publication_title :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :common_title :title)
                             :value (:common_title @form-details)
                             :placeholder (key->placeholder :common_title :title)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :common_title (-> % .-target .-value)])
                             :help (key->help :common_title :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-option-picker
                            {:label (key->title :length :title)
                             :value (:length @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :length (-> % .-target .-value)])
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
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :attributed_author_name (-> % .-target .-value)])
                             :help (key->help :attributed_author_name :title)
                             :disabled @creating?}))}
                {:tab-title "Extra Notes"
                 :content (forms/simple-ff-block
                           (forms/labelled-text-field
                            {:label (key->title :name_category :title)
                             :placeholder (key->placeholder :name_category :title)
                             :value (:name_category @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :name_category (-> % .-target .-value)])
                             :help (key->help :name_category :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :author_of :title)
                             :placeholder (key->placeholder :author_of :title)
                             :value (:author_of @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :author_of (-> % .-target .-value)])
                             :help (key->help :author_of :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :inscribed_author_nationality :title)
                             :placeholder (key->placeholder :inscribed_author_nationality :title)
                             :value (:inscribed_author_nationality @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :inscribed_author_nationality (-> % .-target .-value)])
                             :help (key->help :inscribed_author_nationality :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :inscribed_author_gender :title)
                             :placeholder (key->placeholder :inscribed_author_gender :title)
                             :value (:inscribed_author_gender @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :inscribed_author_gender (-> % .-target .-value)])
                             :help (key->help :inscribed_author_gender :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :information_source :title)
                             :placeholder (key->placeholder :information_source :title)
                             :value (:information_source @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :information_source (-> % .-target .-value)])
                             :help (key->help :information_source :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :additional_info :title)
                             :placeholder (key->placeholder :additional_info :title)
                             :value (:additional_info @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :additional_info (-> % .-target .-value)])
                             :help (key->help :additional_info :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")})
                           (forms/labelled-text-field
                            {:label (key->title :also_published :title)
                             :placeholder (key->placeholder :also_published :title)
                             :value (:also_published @form-details)
                             :on-change #(rf/dispatch [:title/update-new-title-form-field :also_published (-> % .-target .-value)])
                             :help (key->help :also_published :title)
                             :disabled @creating?
                             :class (if @creating? "is-static" "")}))}]})])))
