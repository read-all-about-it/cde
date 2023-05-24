(ns cde.pages.title
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [cde.events]
   [cde.subs]
   [cde.components.metadata :refer [metadata-table simple-metadata-block chapter-table]]))


;;(defn- convert-title-details-to-metadata
;;  "Take the details of a title and convert it to a map of metadata suitable for the 'metadata-table' component."
;;  []
;;  nil)

(defn- convert-length-int-to-string
  "Converts a length integer to a string"
  [length]
  (cond
    (= length 0) "Serialised Title"
    (= length 1) "Short Single Edition"
    (= length 8) "10,000+ Words (Single Edition)"
    :else "Unknown"))

(defn- convert-title-details-to-metadata
  "Takes a title details map and converts it to a vector of maps suitable for the 'metadata-table' component."
  [result]
  (let [structured-results
        [{:title "Publication Title"
          :value (:publication_title result)
          :link (str "#/title/" (:id result))}
         {:title "Common Title"
          :value (:common_title result)
          :link (str "#/title/" (:id result))}
         {:title "Published In"
          :value (:newspaper_title result)
          :link (str "#/newspaper/" (:newspaper_table_id result))}
         {:title "Start Date"
          :value (:span_start result)}
         {:title "End Date"
          :value (:span_end result)}
         {:title "Author"
          :value (:author_common_name result)
          :link (str "#/author/" (:author_id result))}
         {:title "Length"
          :value (convert-length-int-to-string (:length result))}]]
    ; remove all results where the value is nil
    (filter #(not (nil? (:value %))) structured-results)))


(defn title-page
  []
  (r/with-let [loading? (rf/subscribe [:title/loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               title (rf/subscribe [:title/details])
               chapters-in-title (rf/subscribe [:title/chapters])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       (when-not @loading?
         [:div
          [:h1 {:style {:text-align "center"}} (:common_title @title)]
          [:h3 {:style {:text-align "center"}} "(Title Details)"]
          (when @logged-in?
            [:div])
          [metadata-table (convert-title-details-to-metadata @title)]
          (if-not (empty? @chapters-in-title)
            [:div
             [:h3 {:style {:text-align "center"}} "Chapters"]
             [chapter-table @chapters-in-title]]
            [:div
             [:button.button.is-primary
              {:on-click #(rf/dispatch [:title/request-chapters-in-title])}
              "View Chapters"]])])])))