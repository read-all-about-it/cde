(ns cde.components.forms.creation
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]
   [cde.components.forms :as forms]
   [cde.utils :refer [key->help key->title key->placeholder]]))

(defn new-newspaper-form
  "Form for creating a new newspaper record."
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
  "Form for creating a new title record."
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