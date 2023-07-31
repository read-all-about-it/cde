(ns cde.components.creating-records
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [clojure.string :as str]
   [cde.utils :refer [key->help key->title key->placeholder]]))


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