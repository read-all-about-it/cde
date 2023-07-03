(ns cde.components.creating-records
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.utils :refer [key->help key->title]]))

(defn new-title-form
  "Form for creating a new title record."
  []
  (r/with-let [form-details (rf/subscribe [:title/new-title-form])
               creating? (rf/subscribe [:title/create-loading?])
               create-success (rf/subscribe [:title/create-success])
               create-error (rf/subscribe [:title/create-error])
               active-tab-n (r/atom 0)]
    (fn []
      [:div
       [:div

              ;; tabs
        [:div.tabs.is-centered.is-boxed
         [:ul
          [:li {:class (if (= 0 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 0)}]
          [:li {:class (if (= 0 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 1)}
           [:a [:span "Key Details"]]]
          [:li {:class (if (= 1 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 2)}
           [:a [:span "Publication Event"]]]
          [:li {:class (if (= 2 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 3)}
           [:a [:span "Extra Notes"]]]]]
        
        (when (= 0 @active-tab-n)
          [:div
           [:h3 "Connections"]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label
              [:span (key->title :author_id :title)
               [:span.has-text-danger " *"]]]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :class (if @creating? "is-static" (if (str/blank? (:author_id @form-details)) "is-danger" ""))
                              :placeholder "Author ID"
                              :value (:author_id @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :author_id (-> % .-target .-value)])}]]
              [:p.help {:class (if (str/blank? (:author_id @form-details)) "is-danger" "")}
               (str/join " " [(key->help :author_id :title)
                              (when (str/blank? (:author_id @form-details))
                                "This field is required.")])]]]]
           
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label
              [:span (key->title :newspaper_table_id :title)
               [:span.has-text-danger " *"]]]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :class (if @creating? "is-static" (if (str/blank? (:newspaper_table_id @form-details)) "is-danger" ""))
                              :placeholder "Author ID"
                              :value (:newspaper_table_id @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :newspaper_table_id (-> % .-target .-value)])}]]
              [:p.help {:class (if (str/blank? (:newspaper_table_id @form-details)) "is-danger" "")}
               (str/join " " [(key->help :newspaper_table_id :title)
                              (when (str/blank? (:newspaper_table_id @form-details))
                                "This field is required.")])]]]]
           
           ])


        (when (= 1 @active-tab-n)
          [:div
           [:h3 "Key Details"]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label
              [:span (key->title :publication_title :title)
               [:span.has-text-danger " *"]]]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :class (if @creating? "is-static" (if (str/blank? (:publication_title @form-details)) "is-danger" ""))
                              :placeholder "Publication Title"
                              :value (:publication_title @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :publication_title (-> % .-target .-value)])}]]
              [:p.help {:class (if (str/blank? (:publication_title @form-details)) "is-danger" "")}
               (str/join " " [(key->help :publication_title :title)
                              (when (str/blank? (:publication_title @form-details))
                                "This field is required.")])]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :common_title :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :placeholder "Common Title"
                              :value (:common_title @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :common_title (-> % .-target .-value)])}]]
              [:p.help (key->help :common_title :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :span_start :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :class (if (or (str/blank? (:span_start @form-details))
                                             (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
                                       ""
                                       "is-danger")
                              :placeholder "Start Date (eg '1899-01-14')"
                              :value (:span_start @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :span_start (-> % .-target .-value)])}]]
              [:p.help {:class (if (or (str/blank? (:span_start @form-details))
                                       (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
                                 ""
                                 "is-danger")}
               (if (or (str/blank? (:span_start @form-details)) (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
                 (key->help :span_start :title)
                 "Date should be in YYYY-MM-DD format.")]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :span_end :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :class (if (or (str/blank? (:span_end @form-details))
                                             (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
                                       ""
                                       "is-danger")
                              :placeholder "End Date (eg '1901-11-01')"
                              :value (:span_end @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :span_end (-> % .-target .-value)])}]]
              [:p.help {:class (if (or (str/blank? (:span_end @form-details))
                                       (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
                                 ""
                                 "is-danger")}
               (if (or (str/blank? (:span_end @form-details)) (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
                 (key->help :span_end :title)
                 "Date should be in YYYY-MM-DD format.")]]]]
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :length :title)]]
            [:div.field-body
             [:div.field
              [:div.control.has-icons-left
               [:div.select
                [:select
                 {:value (:length @form-details)
                  :on-change #(rf/dispatch [:title/create-edit-title-form-field :length (-> % .-target .-value)])}
                 [:option {:value nil} "Unknown"]
                 [:option {:value 0} "Serialised Title"]
                 [:option {:value 1} "Short Single Edition"]
                 [:option {:value 8} "10,000+ Words (in a Single Edition)"]]
                [:span.icon.is-small.is-left
                 [:i.material-icons "auto_stories"]]]]
              [:p.help (key->help :length :title)]]]]])

        (when (= 2 @active-tab-n)
          [:div
           [:h3 "Publication Event"]
           [:p "When stories are published in newspapers, they sometimes contain additional information about the author or story. For example, they may say \"a new story from the author of 'Dividing Mates'\", or \"from British Author\". You can add these 'publication event' details here."]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :attributed_author_name :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @creating?
                              :class ""
                              :placeholder "eg 'Bill Smith'"
                              :value (:attributed_author_name @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :attributed_author_name (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :attributed_author_name :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :name_category :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "eg Pseudonym, initials, etc"
                              :value (:name_category @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :name_category (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :name_category :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :author_of :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "'Mr Hogarth's Will', 'Hugh Lindsay's Guest'"
                              :value (:author_of @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :author_of (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :author_of :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :inscribed_author_nationality :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "eg 'British'"
                              :value (:inscribed_author_nationality @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :inscribed_author_nationality (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :inscribed_author_nationality :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :inscribed_author_gender :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "eg 'female'"
                              :value (:inscribed_author_gender @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :inscribed_author_gender (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :inscribed_author_gender :title)]]]]])

        (when (= 3 @active-tab-n)

          [:div
           [:h3 "Extra Notes"]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :information_source :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "eg 'Wikipedia', 'Austlit'"
                              :value (:information_source @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :information_source (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :information_source :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :additional_info :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "Possible additional information about this story."
                              :value (:additional_info @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :additional_info (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :additional_info :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :also_published :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "Other (external) sources where this story was published."
                              :value (:also_published @form-details)
                              :on-change #(rf/dispatch [:title/create-edit-title-form-field :also_published (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :also_published :title)]]]]])]

             ;; The 'create Title' Button
       [:div.section
        [:div.block.has-text-right
         [:div.field
          [:a.button.button {:class (str/join " " [(cond @create-success "is-success"
                                                         @create-error "is-danger"
                                                         :else "is-info")
                                                   (when @creating? "is-loading")])
                             :disabled (or @creating? (str/blank? (:publication_title @form-details)))
                             :on-click #(rf/dispatch [:title/create-title @form-details])}
           [:span "Create Title"]
           [:span.icon [:i.material-icons "import_export"]]]
          [:p.help {:class (str/join " " [(cond @create-success "is-success"
                                                @create-error "is-danger"
                                                :else "")])}
           (cond @create-success "Title created successfully!"
                 @create-error "Error updating title. Try again."
                 :else "")]]]]])))