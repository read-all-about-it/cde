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
        ; remove all results where the value is nil and 'keep' is not true
    (filter #(and (not (nil? (:value %))) (not= true (:keep %)))
            structured-results)))


(defn title-page
  []
  (r/with-let [metadata-loading? (rf/subscribe [:title/metadata-loading?])
               chapters-loading? (rf/subscribe [:title/chapters-loading?])
               logged-in? (rf/subscribe [:auth/logged-in?])
               title-metadata (rf/subscribe [:title/details])
               chapters-in-title (rf/subscribe [:title/chapters])
               error (r/atom nil)]
    (fn []
      [:section.section>div.container>div.content
       [:div
        (when-not @metadata-loading?
          [:h1 {:style {:text-align "center"}} (:common_title @title-metadata)])
        (when-not @metadata-loading?
          [:h3 {:style {:text-align "center"}} "Title Metadata"])
        (when-not @metadata-loading?
          [metadata-table (convert-title-details-to-metadata @title-metadata)])
        (when-not @chapters-loading?
          (if-not (empty? @chapters-in-title)
            [:div
             [:h3 {:style {:text-align "center"}} "Discovered Chapters"]
             [chapter-table @chapters-in-title]]
            [:div [:h3 {:style {:text-align "center"}} "No Chapters Found in this title record!"]]))
        (when (empty? @chapters-in-title)
          [:div
           [:button.button.is-primary
            {:on-click #(rf/dispatch [:title/request-chapters-in-title])}
            "View Chapters"]])]])))