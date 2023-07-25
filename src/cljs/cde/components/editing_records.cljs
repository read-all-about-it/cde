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
                :on-click #(reset! active-tab-n 1)}
           [:a [:span "Extra Notes"]]]]]


        (when (= 0 @active-tab-n)
          [:div
           [:h3 "Key Details"]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :chapter_title :chapter)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "The Chapter Title"
                              :value (:chapter_title @form-details)
                              :on-change #(rf/dispatch [:chapter/update-edit-chapter-form-field :chapter_title (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :chapter_title :chapter)]]]]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :chapter_number :chapter)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @updating?
                              :placeholder "Chapter Number (eg 'XII')"
                              :value (:chapter_number @form-details)
                              :on-change #(rf/dispatch [:chapter/update-edit-chapter-form-field :chapter_number (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :chapter_number :chapter)]]]]])

        (when (= 1 @active-tab-n)
          [:div
           [:h3 "Extra Notes"]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :final_date :chapter)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :placeholder "Publication Date in YYYY-MM-DD format (eg '2018-01-01')"
                              :value (:final_date @form-details)
                              :on-change #(rf/dispatch [:chapter/update-edit-chapter-form-field :final_date (-> % .-target .-value)])}]]
              (if (and (not (nil? (:final_date @form-details)))
                       (not (re-matches #"\d{4}-\d{2}-\d{2}" (:final_date @form-details))))
                [:p.help.is-danger "Date should be in YYYY-MM-DD format."]
                [:p.help "This is the publication date for the chapter. This is usually the date of the newspaper issue in which the chapter was published."])]]]])]
       

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