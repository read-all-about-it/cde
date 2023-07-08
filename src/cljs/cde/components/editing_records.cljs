(ns cde.components.editing-records
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.utils :refer [key->help key->title]]))

(defn edit-gender-options
  "A component for setting the 'author gender' option in an edit block."
  []
  (r/with-let [default-genders ["Male" "Female" "Unknown" "Multiple" "Both"]
               form-details (rf/subscribe [:author/edit-author-form])]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label [:span (key->title :gender :author)]]]
     [:div.field-body
      [:div.field
       [:div.control.has-icons-left
        [:div.select
         (apply
          vector
          (concat [:select]
                  [{:value (:gender @form-details)
                    :on-change #(rf/dispatch [:author/update-edit-author-form-field :gender (-> % .-target .-value)])}]
                  [[:option {:value "" :disabled true :selected true} "Author Gender"]]
                  (map (fn [g] [:option {:value g} g]) default-genders)))
         [:span.icon.is-small.is-left
          [:i.material-icons "transgender"]]]]

       [:p.help (key->help :gender :author)]]]]))



(defn edit-title-form
  "Form for editing an existing title"
  []
  (r/with-let [form-details (rf/subscribe [:title/edit-title-form])
               updating? (rf/subscribe [:title/update-loading?])
               update-success (rf/subscribe [:title/update-success])
               update-error (rf/subscribe [:title/update-error])
               active-tab-n (r/atom 0)]
    (fn []
      [:div
       [:div

        ;; tabs
        [:div.tabs.is-centered.is-boxed
         [:ul
          [:li {:class (if (= 0 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 0)}
           [:a [:span "Key Details"]]]
          [:li {:class (if (= 1 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 1)}
           [:a [:span "Publication Event"]]]
          [:li {:class (if (= 2 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 2)}
           [:a [:span "Extra Notes"]]]]]


        (when (= 0 @active-tab-n)
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
                              :disabled @updating?
                              :class (if @updating? "is-static" (if (str/blank? (:publication_title @form-details)) "is-danger" ""))
                              :placeholder "Publication Title"
                              :value (:publication_title @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :publication_title (-> % .-target .-value)])}]]
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
                              :disabled @updating?
                              :placeholder "Common Title"
                              :value (:common_title @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :common_title (-> % .-target .-value)])}]]
              [:p.help (key->help :common_title :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :span_start :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @updating?
                              :class (if (or (str/blank? (:span_start @form-details))
                                             (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
                                       ""
                                       "is-danger")
                              :placeholder "Start Date (eg '1899-01-14')"
                              :value (:span_start @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :span_start (-> % .-target .-value)])}]]
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
                              :disabled @updating?
                              :class (if (or (str/blank? (:span_end @form-details))
                                             (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
                                       ""
                                       "is-danger")
                              :placeholder "End Date (eg '1901-11-01')"
                              :value (:span_end @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :span_end (-> % .-target .-value)])}]]
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
                  :on-change #(rf/dispatch [:title/update-edit-title-form-field :length (-> % .-target .-value)])}
                 [:option {:value nil} "Unknown"]
                 [:option {:value 0} "Serialised Title"]
                 [:option {:value 1} "Short Single Edition"]
                 [:option {:value 8} "10,000+ Words (in a Single Edition)"]]
                [:span.icon.is-small.is-left
                 [:i.material-icons "auto_stories"]]]]
              [:p.help (key->help :length :title)]]]]])

        (when (= 1 @active-tab-n)
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
                              :disabled @updating?
                              :class ""
                              :placeholder "eg 'Bill Smith'"
                              :value (:attributed_author_name @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :attributed_author_name (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :attributed_author_name :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :name_category :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "eg Pseudonym, initials, etc"
                              :value (:name_category @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :name_category (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :name_category :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :author_of :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "'Mr Hogarth's Will', 'Hugh Lindsay's Guest'"
                              :value (:author_of @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :author_of (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :author_of :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :inscribed_author_nationality :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "eg 'British'"
                              :value (:inscribed_author_nationality @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :inscribed_author_nationality (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :inscribed_author_nationality :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :inscribed_author_gender :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "eg 'female'"
                              :value (:inscribed_author_gender @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :inscribed_author_gender (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :inscribed_author_gender :title)]]]]])

        (when (= 2 @active-tab-n)

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
                              :disabled @updating?
                              :placeholder "eg 'Wikipedia', 'Austlit'"
                              :value (:information_source @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :information_source (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :information_source :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :additional_info :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "Possible additional information about this story."
                              :value (:additional_info @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :additional_info (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :additional_info :title)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :also_published :title)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "Other (external) sources where this story was published."
                              :value (:also_published @form-details)
                              :on-change #(rf/dispatch [:title/update-edit-title-form-field :also_published (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :also_published :title)]]]]])]

       ;; The 'Update Title' Button
       [:div.section
        [:div.block.has-text-right
         [:div.field
          [:a.button.button {:class (str/join " " [(cond @update-success "is-success"
                                                         @update-error "is-danger"
                                                         :else "is-info")
                                                   (when @updating? "is-loading")])
                             :disabled (or @updating? (str/blank? (:publication_title @form-details)))
                             :on-click #(rf/dispatch [:title/update-title @form-details])}
           [:span "Update Title"]
           [:span.icon [:i.material-icons "import_export"]]]
          [:p.help {:class (str/join " " [(cond @update-success "is-success"
                                                @update-error "is-danger"
                                                :else "")])}
           (cond @update-success "Title updated successfully!"
                 @update-error "Error updating title. Try again."
                 :else "")]]]]])))





(defn edit-chapter-form
  "Form for editing the metadata of an existing chapter."
  []
  (r/with-let [form-details (rf/subscribe [:chapter/edit-chapter-form])
               updating? (rf/subscribe [:chapter/update-loading?])
               update-success (rf/subscribe [:chapter/update-success])
               update-error (rf/subscribe [:chapter/update-error])
               active-tab-n (r/atom 0)]
    (fn []
      [:div
       [:div

     ;; tabs
        [:div.tabs.is-centered.is-boxed
         [:ul
          [:li {:class (if (= 0 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 0)}
           [:a [:span "Key Details"]]]
          [:li {:class (if (= 1 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 2)}
           [:a [:span "Extra Notes"]]]]]


        (when (= 0 @active-tab-n)
          [:div
           [:h3 "Key Details"]])]

       ;; The 'Update Chapter' Button
       [:div.section
        [:div.block.has-text-right
         [:div.field
          [:a.button.button {:class (str/join " " [(cond @update-success "is-success"
                                                         @update-error "is-danger"
                                                         :else "is-info")
                                                   (when @updating? "is-loading")])
                             :disabled @updating?
                             :on-click #(rf/dispatch [:chapter/update-chapter @form-details])}
           [:span "Update Chapter"]
           [:span.icon [:i.material-icons "import_export"]]]
          [:p.help {:class (str/join " " [(cond @update-success "is-success"
                                                @update-error "is-danger"
                                                :else "")])}
           (cond @update-success "Chapter updated successfully!"
                 @update-error "Error updating chapter. Try again."
                 :else "")]]]]])))


(defn edit-author-form
  "Form for editing an existing author"
  []
  (r/with-let [form-details (rf/subscribe [:author/edit-author-form])
               updating? (rf/subscribe [:author/update-loading?])
               update-success (rf/subscribe [:author/update-success])
               update-error (rf/subscribe [:author/update-error])
               active-tab-n (r/atom 0)]
    (fn []
      [:div
       [:div

          ;; tabs
        [:div.tabs.is-centered.is-boxed
         [:ul
          [:li {:class (if (= 0 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 0)}
           [:a [:span "Name"]]]
          [:li {:class (if (= 1 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 1)}
           [:a [:span "Nationality"]]]
          [:li {:class (if (= 2 @active-tab-n) "is-active" "")
                :on-click #(reset! active-tab-n 2)}
           [:a [:span "Other"]]]]]


        (when (= 0 @active-tab-n)
          [:div
           [:h3 "Name"]

           ;; common name
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label
              [:span (key->title :common_name :author)
               [:span.has-text-danger " *"]]]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :disabled @updating?
                              :class (if @updating? "is-static" (if (str/blank? (:common_name @form-details)) "is-danger" ""))
                              :placeholder "Author Common Name (eg 'Palmer-Archer, Laura M.')"
                              :value (:common_name @form-details)
                              :on-change #(rf/dispatch [:author/update-edit-author-form-field :common_name (-> % .-target .-value)])}]]
              [:p.help {:class (if (str/blank? (:common_name @form-details)) "is-danger" "")}
               (str/join " " [(key->help :common_name :author)
                              (when (str/blank? (:common_name @form-details))
                                "This field is required.")])]]]]

           ;; other names
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :other_name :author)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "Other names and pseudonyms (eg 'Bushwoman')"
                              :value (:other_name @form-details)
                              :on-change #(rf/dispatch [:author/update-edit-author-form-field :other_name (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :other_name :author)]]]]])

        (when (= 1 @active-tab-n)
          [:div
           [:h3 "Nationality"]
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :nationality :author)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "(eg 'Australian')"
                              :value (:nationality @form-details)
                              :on-change #(rf/dispatch [:author/update-edit-author-form-field :nationality (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :nationality :author)]]]]
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :nationality_details :author)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "(eg 'Born in Cairns')"
                              :value (:nationality_details @form-details)
                              :on-change #(rf/dispatch [:author/update-edit-author-form-field :nationality_details (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :nationality_details :author)]]]]])

        (when (= 2 @active-tab-n)
          [:div [:h3 "Other Details"]

           [edit-gender-options]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :author_details :author)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "(eg 'Austlit')"
                              :value (:author_details @form-details)
                              :on-change #(rf/dispatch [:author/update-edit-author-form-field :author_details (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :author_details :author)]]]]])


         ;; The 'Update Author' Button
        [:div.section
         [:div.block.has-text-right
          [:div.field
           [:a.button.button {:class (str/join " " [(cond @update-success "is-success"
                                                          @update-error "is-danger"
                                                          :else "is-info")
                                                    (when @updating? "is-loading")])
                              :disabled (or @updating? (str/blank? (:common_name @form-details)))
                              :on-click #(rf/dispatch [:author/update-author @form-details])}
            [:span "Update Author"]
            [:span.icon [:i.material-icons "import_export"]]]
           [:p.help {:class (str/join " " [(cond @update-success "is-success"
                                                 @update-error "is-danger"
                                                 :else "")])}
            (cond @update-success "Author updated successfully!"
                  @update-error "Error updating author. Try again."
                  :else "")]]]]]])))


(defn edit-newspaper-form
  []
  [:p ""]
  )