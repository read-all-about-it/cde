(ns cde.components.creating-records
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.components.modals :refer [modal-button]]
   [cde.utils :refer [key->help key->title]]))







(defn lookup-newspaper-button
  "A button for looking up a newspaper by triggering a modal.
   Takes an 'on-pick' argument, which is a function that will be
    called when the user picks a newspaper from the list."
  []
  (r/with-let [newspapers (rf/subscribe [:platform/all-newspapers])
               search-text (r/atom "")]
    [modal-button :lookup-newspaper-modal
     ;; title
     "Find A Newspaper"
     ;; body 
     [:div
      (for [n @newspapers]
        (when (str/includes? (str/lower-case (:title n)) (str/lower-case @search-text))
          [:div.block
           [:button.button
            {:on-click #(do
                          (rf/dispatch [:title/update-new-title-form-field :newspaper_table_id (:id n)])
                          (rf/dispatch [:app/hide-modal :lookup-newspaper-modal]))}
            (:title n)]
           
           ]))]
     ;; footer
     [:div
      [:div.field.is-fullwidth
       [:div.control
        [:input.input {:type "text"
                       :placeholder "Search"
                       :value @search-text
                       :on-change #(reset! search-text (-> % .-target .-value))}]]]]
     "is-info"]))

(defn lookup-author-button
  "A button for looking up a newspaper by triggering a modal.
     Takes an 'on-pick' argument, which is a function that will be
      called when the user picks a newspaper from the list."
  []
  (r/with-let [authors (rf/subscribe [:platform/all-authors])
               search-text (r/atom "")]
    [modal-button :lookup-author-modal
       ;; title
     "Find An Author"
       ;; body 
     [:div
      (for [n @authors]
        (when (str/includes? (str/lower-case (:common_name n)) (str/lower-case @search-text))
          [:div.block
           [:button.button
            {:on-click #(do
                          (rf/dispatch [:title/update-new-title-form-field :author_id (:id n)])
                          (rf/dispatch [:app/hide-modal :lookup-author-modal]))}
            (:common_name n)]]))]
       ;; footer
     [:div
      [:div.field.is-fullwidth
       [:div.control
        [:input.input {:type "text"
                       :placeholder "Search"
                       :value @search-text
                       :on-change #(reset! search-text (-> % .-target .-value))}]]]]
     "is-info"]))


(defn new-title-form
  "Form for creating a new title record."
  []
  (r/with-let [form-details (rf/subscribe [:title/new-title-form])
               creating? (rf/subscribe [:title/creation-loading?])
               create-success (rf/subscribe [:title/creation-success])
               create-error (rf/subscribe [:title/creation-error])
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

        ;;    choose the newspaper
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label
              [:span (key->title :newspaper_table_id :title)
               [:span.has-text-danger " *"]]]]
            [:div.field-body
             [:div.field
              [:div.field.has-addons
               [:div.control
                [:input.input {:type "text"
                               :disabled @creating?
                               :class (if @creating? "is-static" (if (str/blank? (:newspaper_table_id @form-details)) "is-danger" ""))
                               :placeholder "Newspaper ID"
                               :value (:newspaper_table_id @form-details)
                               :on-change #(rf/dispatch [:title/update-new-title-form-field :newspaper_table_id (-> % .-target .-value)])
                               :on-blur #(rf/dispatch [:newspaper/get-newspaper (:newspaper_table_id @form-details)])}]]
               [lookup-newspaper-button]]
              [:p.help {:class (if (str/blank? (:newspaper_table_id @form-details)) "is-danger" "")}
               (str/join " " [(key->help :newspaper_table_id :title)
                              (when (str/blank? (:newspaper_table_id @form-details))
                                "This field is required.")])]]]]
           
           ;;   choose the author
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label
              [:span (key->title :author_id :title)
               [:span.has-text-danger " *"]]]]
            [:div.field-body
             [:div.field
              [:div.field.has-addons
               [:div.control
                [:input.input {:type "text"
                               :disabled @creating?
                               :class (if @creating? "is-static" (if (str/blank? (:author_id @form-details)) "is-danger" ""))
                               :placeholder "Author ID"
                               :value (:author_id @form-details)
                               :on-change #(rf/dispatch [:title/update-new-title-form-field :author_id (-> % .-target .-value)])
                               :on-blur #(rf/dispatch [:author/get-author (:author_id @form-details)])}]]
               [lookup-author-button]]
              [:p.help {:class (if (str/blank? (:author_id @form-details)) "is-danger" "")}
               (str/join " " [(key->help :author_id :title)
                              (when (str/blank? (:author_id @form-details))
                                "This field is required.")])]]]]





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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :publication_title (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :common_title (-> % .-target .-value)])}]]
              [:p.help (key->help :common_title :title)]]]]

          ;;  [:div.field.is-horizontal
          ;;   [:div.field-label.is-normal
          ;;    [:label.label (key->title :span_start :title)]]
          ;;   [:div.field-body
          ;;    [:div.field
          ;;     [:div.control
          ;;      [:input.input {:type "text"
          ;;                     :disabled @creating?
          ;;                     :class (if (or (str/blank? (:span_start @form-details))
          ;;                                    (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
          ;;                              ""
          ;;                              "is-danger")
          ;;                     :placeholder "Start Date (eg '1899-01-14')"
          ;;                     :value (:span_start @form-details)
          ;;                     :on-change #(rf/dispatch [:title/update-new-title-form-field :span_start (-> % .-target .-value)])}]]
          ;;     [:p.help {:class (if (or (str/blank? (:span_start @form-details))
          ;;                              (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
          ;;                        ""
          ;;                        "is-danger")}
          ;;      (if (or (str/blank? (:span_start @form-details)) (re-matches #"\d{4}-\d{2}-\d{2}" (:span_start @form-details)))
          ;;        (key->help :span_start :title)
          ;;        "Date should be in YYYY-MM-DD format.")]]]]

          ;;  [:div.field.is-horizontal
          ;;   [:div.field-label.is-normal
          ;;    [:label.label (key->title :span_end :title)]]
          ;;   [:div.field-body
          ;;    [:div.field
          ;;     [:div.control
          ;;      [:input.input {:type "text"
          ;;                     :disabled @creating?
          ;;                     :class (if (or (str/blank? (:span_end @form-details))
          ;;                                    (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
          ;;                              ""
          ;;                              "is-danger")
          ;;                     :placeholder "End Date (eg '1901-11-01')"
          ;;                     :value (:span_end @form-details)
          ;;                     :on-change #(rf/dispatch [:title/update-new-title-form-field :span_end (-> % .-target .-value)])}]]
          ;;     [:p.help {:class (if (or (str/blank? (:span_end @form-details))
          ;;                              (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
          ;;                        ""
          ;;                        "is-danger")}
          ;;      (if (or (str/blank? (:span_end @form-details)) (re-matches #"\d{4}-\d{2}-\d{2}" (:span_end @form-details)))
          ;;        (key->help :span_end :title)
          ;;        "Date should be in YYYY-MM-DD format.")]]]]
           
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :length :title)]]
            [:div.field-body
             [:div.field
              [:div.control.has-icons-left
               [:div.select
                [:select
                 {:value (:length @form-details)
                  :on-change #(rf/dispatch [:title/update-new-title-form-field :length (-> % .-target .-value)])}
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
                              :disabled @creating?
                              :class ""
                              :placeholder "eg 'Bill Smith'"
                              :value (:attributed_author_name @form-details)
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :attributed_author_name (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :name_category (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :author_of (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :inscribed_author_nationality (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :inscribed_author_gender (-> % .-target .-value)])}]]
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
                              :disabled @creating?
                              :placeholder "eg 'Wikipedia', 'Austlit'"
                              :value (:information_source @form-details)
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :information_source (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :additional_info (-> % .-target .-value)])}]]
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
                              :on-change #(rf/dispatch [:title/update-new-title-form-field :also_published (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :also_published :title)]]]]])]

       ;; The 'create Title' Button
       [:div.section
        [:div.block.has-text-right
         (if-not @create-success
           [:div.field
            [:a.button.button {:class (str/join " " [(cond @create-error "is-danger"
                                                           :else "is-info")
                                                     (when @creating? "is-loading")])
                               :disabled (or @creating? (str/blank? (:publication_title @form-details)))
                               :on-click #(rf/dispatch [:title/create-new-title @form-details])}
             (cond @create-error [:span "Error creating title. Try again."]
                   :else [:span "Create Title"])
             (if @create-error
               [:span.icon [:i.material-icons "error"]]
               [:span.icon [:i.material-icons "add"]])]
            [:p.help {:class (str/join " " [(cond @create-error "is-danger"
                                                  :else "")])}
             (cond @create-error "Error creating title. Try again."
                   :else "")]]
           [:div.field
            [:a.button.button {:class "is-success"
                               :href (str "#/title/" (:id @create-success))}
             [:span "Title created!"]
             [:span.icon [:i.material-icons "done"]]]
            [:p.help {:class "is-success"} "Title created! Click to see it."]])]]])))




(defn new-newspaper-form
  []
  [:p "Test"])