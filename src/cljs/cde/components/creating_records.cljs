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




(defn new-author-form
  "Form for creating a new author"
  []
  (r/with-let [form-details (rf/subscribe [:author/new-author-form])
               creating? (rf/subscribe [:author/creation-loading?])
               create-success (rf/subscribe [:author/creation-success])
               create-error (rf/subscribe [:author/creation-error])
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
                              :disabled @creating?
                              :class (if @creating? "is-static" (if (str/blank? (:common_name @form-details)) "is-danger" ""))
                              :placeholder "Author Common Name (eg 'Palmer-Archer, Laura M.')"
                              :value (:common_name @form-details)
                              :on-change #(rf/dispatch [:author/update-new-author-form-field :common_name (-> % .-target .-value)])}]]
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
                              :disabled @creating?
                              :placeholder "Other names and pseudonyms (eg 'Bushwoman')"
                              :value (:other_name @form-details)
                              :on-change #(rf/dispatch [:author/update-new-author-form-field :other_name (-> % .-target .-value)])}]]
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
                              :disabled @creating?
                              :placeholder "(eg 'Australian')"
                              :value (:nationality @form-details)
                              :on-change #(rf/dispatch [:author/update-new-author-form-field :nationality (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :nationality :author)]]]]
           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :nationality_details :author)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "(eg 'Born in Cairns')"
                              :value (:nationality_details @form-details)
                              :on-change #(rf/dispatch [:author/update-new-author-form-field :nationality_details (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :nationality_details :author)]]]]])

        (when (= 2 @active-tab-n)
          [:div [:h3 "Other Details"]

            ;;  [edit-gender-options]

           [:div.field.is-horizontal
            [:div.field-label.is-normal
             [:label.label (key->title :author_details :author)]]
            [:div.field-body
             [:div.field
              [:div.control
               [:input.input {:type "text"
                              :class ""
                              :disabled @creating?
                              :placeholder "(eg 'Austlit')"
                              :value (:author_details @form-details)
                              :on-change #(rf/dispatch [:author/update-new-author-form-field :author_details (-> % .-target .-value)])}]]
              [:p.help {:class ""} (key->help :author_details :author)]]]]])

          ;; The 'Create Author' Button
        [:div.section
         [:div.block.has-text-right
          (if-not @create-success
            [:div.field
             [:a.button.button {:class (str/join " " [(cond @create-error "is-danger"
                                                            :else "is-info")
                                                      (when @creating? "is-loading")])
                                :disabled (or @creating? (str/blank? (:common_name @form-details)))
                                :on-click #(rf/dispatch [:author/create-new-author @form-details])}
              (cond @create-error [:span "Try again..."]
                    :else [:span "Create Author"])
              (if @create-error
                [:span.icon [:i.material-icons "error"]]
                [:span.icon [:i.material-icons "add"]])]

             [:p.help {:class (str/join " " [(cond @create-error "is-danger"
                                                   :else "")])}
              (cond @create-error "Error creating author. Try again."
                    :else "")]]
            [:div.field
             [:a.button.button {:class "is-success"
                                :href (str "#/author/" (:id @create-success))}
              [:span "Author created!"]
              [:span.icon [:i.material-icons "done"]]]
             [:p.help {:class "is-success"} "Author created! Click to see it."]])]]]])))



(defn- trove-article-id-and-title-id-block
  "A form block for entering a Trove Article ID and a Title ID in the process
   of creating a new chapter."
  []
  (r/with-let [form-details (rf/subscribe [:chapter/new-chapter-form])
               ;; connection to the trove API:
               trove-loading? (rf/subscribe [:trove/loading?])
               trove-error (rf/subscribe [:trove/error])
               trove-details (rf/subscribe [:trove/details])
               ;; connection to our database (for details of the title we're adding a chapter to):
               title-loading? (rf/subscribe [:title/metadata-loading?])
               title-error (rf/subscribe [:title/error])
               title-details (rf/subscribe [:title/details])
               ;; trove_article_ids of chapters known to be in our db already:
               existing-chapter-trove-article-ids (rf/subscribe [:trove/chapter-exists-list])]
    (fn []
      [:div.block
       ;; get the user to input the trove_article_id for the chapter they're adding
       [:div.field.is-horizontal
        [:div.field-label.is-normal
         [:label.label "Trove Article ID"]]
        [:div.field-body
         [:div.field
          [:div.field.has-addons
           [:div.control
            [:input.input {:type "text"
                           :class (str/join " " [(cond (str/blank? (str (:trove_article_id @form-details))) ""
                                                       (not (re-matches #"\d+" (str (:trove_article_id @form-details)))) "is-danger"
                                                       (and (not (nil? @trove-error)) (string? @trove-error)) "is-danger"
                                                       (some #(= (:trove_article_id @form-details) %) (map #(str %) @existing-chapter-trove-article-ids)) "is-danger"
                                                       (and (false? @trove-loading?) (not (nil? @trove-details)) (= (:trove_article_id @form-details) (str (:trove_article_id @trove-details)))) "is-success"
                                                       :else "is-info")
                                                 (when @trove-loading?
                                                   "is-loading")])
                           :placeholder "12345678"
                           :value (:trove_article_id @form-details)
                           :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :trove_article_id (-> % .-target .-value)])}]]
           [:div.control
            [:button.button {:class (str/join " " [(cond (str/blank? (str (:trove_article_id @form-details))) "is-static"
                                                         (not (re-matches #"\d+" (str (:trove_article_id @form-details)))) "is-danger"
                                                         (and (not (nil? @trove-error)) (string? @trove-error)) "is-danger"
                                                         (some #(= (:trove_article_id @form-details) %) (map #(str %) @existing-chapter-trove-article-ids)) "is-danger"
                                                         (and (false? @trove-loading?) (not (nil? @trove-details)) (= (:trove_article_id @form-details) (str (:trove_article_id @trove-details)))) "is-success"
                                                         :else "is-info")
                                                   (when @trove-loading?
                                                     "is-loading")])
                             :on-click #(rf/dispatch [:trove/get-chapter (:trove_article_id @form-details)])}
             (if @trove-details [:span "Update Details from Trove"] [:span "Get Details from Trove"])]]]
          (cond
            @trove-loading?
            [:p.help.is-info "Checking Trove..."]

            (and (not (str/blank? (str (:trove_article_id @form-details))))
                 (not (re-matches #"\d+" (str (:trove_article_id @form-details)))))
            [:p.help.is-danger "Trove ID must be a number."]

            (and (not (nil? @trove-error)) (string? @trove-error))
            [:p.help.is-danger @trove-error]

            (some #(= (:trove_article_id @form-details) %) (map #(str %) @existing-chapter-trove-article-ids))
            [:p.help.is-danger "A chapter with this Trove ID is already in the database!"]

            (and (false? @trove-loading?) (not (nil? @trove-details)) (= (:trove_article_id @form-details) (str (:trove_article_id @trove-details))))
            [:p.help.is-success "We found this chapter in Trove!"]

            :else [:p.help "This is the 'Article ID' (from Trove) for the chapter you're adding."])]]]
       ;; get the user to input the title_id for the chapter they're adding
       ;; this should be populated if they come to view from *most* 'add chapter' links
       [:div.field.is-horizontal
        [:div.field-label.is-normal
         [:label.label "Title ID"]]
        [:div.field-body
         [:div.field
          [:div.field.has-addons
           [:div.control
            [:input.input {:type "text"
                           :class (str/join " " [(cond (str/blank? (str (:title_id @form-details))) ""
                                                       (not (re-matches #"\d+" (str (:title_id @form-details)))) "is-danger"
                                                       (and (not (nil? @title-error)) (string? @title-error)) "is-danger"
                                                       (and (false? @title-loading?) (not (nil? @title-details)) (= (:title_id @form-details) (str (:id @title-details)))) "is-success"
                                                       :else "is-info")
                                                 (when @title-loading?
                                                   "is-loading")])
                           :placeholder "1234"
                           :value (:title_id @form-details)
                           :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :title_id (-> % .-target .-value)])}]]
           [:div.control
            [:button.button {:class (str/join " " [(cond (str/blank? (str (:title_id @form-details))) "is-static"
                                                         (not (re-matches #"\d+" (str (:title_id @form-details)))) "is-danger"
                                                         (and (not (nil? @title-error)) (string? @title-error)) "is-danger"
                                                         (and (false? @title-loading?) (not (nil? @title-details)) (= (:title_id @form-details) (str (:id @title-details)))) "is-success"
                                                         :else "is-info")
                                                   (when @title-loading?
                                                     "is-loading")])
                             :on-click #(rf/dispatch [:title/get-title (:title_id @form-details)])}
             [:span "Check Title Record"]]]]
          (cond
            @title-loading?
            [:p.help.is-info "Checking Title..."]
            (and (not (str/blank? (str (:title_id @form-details))))
                 (not (re-matches #"\d+" (str (:title_id @form-details)))))
            [:p.help.is-danger "Title ID must be a number."]
            (and (not (nil? @title-error)) (string? @title-error) (not (str/includes? @title-error "Unknown")))
            [:p.help.is-danger @title-error]
            (and (not (nil? @title-error)) (string? @title-error))
            [:p.help.is-danger (str @title-error " - please check the Title ID and try again.")]
            (and (false? @title-loading?) (not (nil? @title-details)) (= (:title_id @form-details) (str (:id @title-details))))
            [:p.help.is-success "The title you're adding a chapter to exists in our database!"]
            :else
            [:p.help "This is the 'Title ID' (from our database) for the title/story to which you're adding the chapter."])]]]])))


(defn new-chapter-form
  "Form for creating a new chapter"
  []
  (r/with-let [form-details (rf/subscribe [:chapter/new-chapter-form])
               ;; connection to the trove API:
               trove-loading? (rf/subscribe [:trove/loading?])
               trove-error (rf/subscribe [:trove/error])
               trove-details (rf/subscribe [:trove/details])
               ;; connection to our database (for details of the title we're adding a chapter to):
               title-loading? (rf/subscribe [:title/metadata-loading?])
               title-error (rf/subscribe [:title/error])
               title-details (rf/subscribe [:title/details])
               ;; trove_article_ids of chapters known to be in our db already:
               existing-chapter-trove-article-ids (rf/subscribe [:trove/chapter-exists-list])
               ;; creation details
               creation-loading? (rf/subscribe [:chapter/creation-loading?])
               creation-error (rf/subscribe [:chapter/creation-error])
               created-chapter-details (rf/subscribe [:chapter/creation-success])]
    (fn []
      [:div
       [trove-article-id-and-title-id-block] ;; block with two main fields, getting & checking trove_article_id and title_id

       ;; THE 'Check/Populated/Submit' Button
       (when (and (= (:title_id @form-details) (str (:id @title-details))) ;; title_id is populated and valid
                  (= (:trove_article_id @form-details) (str (:trove_article_id @trove-details))) ;; trove_article_id is populated and valid
                  (not (some #(= (:trove_article_id @form-details) %) (map #(str %) @existing-chapter-trove-article-ids)))) ;; trove_article_id is not already in our db
         [:div.block.has-text-centered
          (if-not (or (not (nil? (:chapter_title @form-details)))
                      (not (nil? (:chapter_number @form-details))))
            [:button.button
             {:on-click #(rf/dispatch [:chapter/populate-new-chapter-form])}
             [:span "Check Details"]]
            ;; looks like we have all the details we need, so we can submit the form:
            (cond @creation-error
                  [:button.button {:class "is-danger"
                                   :on-click #(rf/dispatch [:chapter/create-new-chapter @form-details])}
                   [:span "Submit"]]
                  @created-chapter-details ;; we've created the chapter successfully, so nav to it
                  [:a.button.button {:class "is-success"
                                     :href (str "#/chapter/" (:id @created-chapter-details))}
                   [:span "Chapter Created! Go to Chapter"]]
                  :else ;; we're ready to create the chapter
                  [:button.button
                   {:class (if @creation-loading? "is-loading" "is-info")
                    :on-click #(rf/dispatch [:chapter/create-new-chapter @form-details])}
                   [:span "Submit"]]))])

       (when (or (not (nil? (:chapter_title @form-details)))
                 (not (nil? (:chapter_number @form-details))))
         [:div.block
          [:h3 {:style {:text-align "center"}} "Chapter Details"]

          ;; Chapter Title
          [:div.field.is-horizontal
           [:div.field-label.is-normal
            [:label.label "Chapter Title"]]
           [:div.field-body
            [:div.field
             [:div.control
              [:input.input {:type "text"
                             :placeholder "The Chapter Title"
                             :value (:chapter_title @form-details)
                             :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :chapter_title (-> % .-target .-value)])}]]
             [:p.help "This is the chapter title for the chapter you're adding."]]]]

          ;; Chapter Number
          [:div.field.is-horizontal
           [:div.field-label.is-normal
            [:label.label "Chapter Number"]]
           [:div.field-body
            [:div.field
             [:div.control
              [:input.input {:type "text"
                             :placeholder "Chapter Number (eg 'XII')"
                             :value (:chapter_number @form-details)
                             :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :chapter_number (-> % .-target .-value)])}]]
             [:p.help "This is the chapter number for the chapter you're adding. This is usually a roman numeral (eg 'XII')."]]]]

          ;; Final Date (ie, publication date)
          [:div.field.is-horizontal
           [:div.field-label.is-normal
            [:label.label "Publication Date"]]
           [:div.field-body
            [:div.field
             [:div.control
              [:input.input {:type "text"
                             :placeholder "Publication Date in YYYY-MM-DD format (eg '2018-01-01')"
                             :value (:final_date @form-details)
                             :on-change #(rf/dispatch [:chapter/update-new-chapter-form-field :final_date (-> % .-target .-value)])}]]
             (if (and (not (nil? (:final_date @form-details)))
                      (not (re-matches #"\d{4}-\d{2}-\d{2}" (:final_date @form-details))))
               [:p.help.is-danger "Date should be in YYYY-MM-DD format."]
               [:p.help "This is the publication date for the chapter you're adding. This is usually the date of the newspaper issue in which the chapter was published."])]]]])])))