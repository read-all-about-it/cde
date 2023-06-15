(ns cde.components.forms
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]))

(defn new-title-form
  "Form for creating a new title"
  []
  (r/with-let [newspapers nil ;(rf/subscribe [:platform/newspapers])
               default-newspapers [{:id 1 :common_title "Test"}]
               form-details (rf/subscribe [:title/new-title-form])]
    (fn []
      [:div
       
       
       [:div.field
        [:label.label "Newspaper"]
        [:div.control
         [:div.select
          (apply vector (concat [:select]
                                [{:value (:newspaper_table_id @form-details)
                                  :on-change #(rf/dispatch [:rf/dispatch [:title/update-new-title-form-field :newspaper_table_id (-> % .-target .-value)]])}]
                                (map (fn [newspaper]
                                       [:option {:value (:id newspaper)} (:common_title newspaper)])
                                     default-newspapers)))]]
        [:p.help
         [:span "This is the newspaper that the story was published in."]]]



       [:div.field
        [:label.label "Publication Title"]
        [:div.control
         [:input.input
          {:type "text"
           :placeholder "Publication Title"
           :value (:publication_title @form-details)
           :on-change #(rf/dispatch [:title/update-new-title-form-field :publication (-> % .-target .-value)])}]]
        [:p.help
         (when-not (empty? (:publication_title @form-details))
           [:span "This is the title the story was published under in this particular instance."])]]


       [:div.field
        [:label.label "Common Title"]
        [:div.control
         [:input.input
          {:type "text"
           :placeholder "Common Title"
           :value (:common_title @form-details)
           :on-change #(rf/dispatch [:title/update-new-title-form-field :common_title (-> % .-target .-value)])}]]
        [:p.help
         (when-not (empty? (:common_title @form-details))
           [:span "This is the title that the story is 'commonly known as', even if some newspapers published it under a different title."])]]
       
       
       ]
      )))